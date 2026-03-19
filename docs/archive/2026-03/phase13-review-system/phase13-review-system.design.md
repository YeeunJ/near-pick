# Design: phase13-review-system

## Overview

Phase 12에서 완료된 Reservation(COMPLETED) · FlashPurchase(PICKED_UP) 를 진입점으로,
소비자 리뷰 작성 → Claude AI 비동기 자동검증 → 상품 평점 집계 → 소상공인 답글 → 신고·관리자 큐 플로우를 구현한다.

---

## 1. State Diagrams

### ReviewStatus 전이

```
작성 요청
    ↓
ACTIVE (status=ACTIVE, aiChecked=false, blindPending=false)
    ↓  (비동기 AI 검증)
┌─── pass      → ACTIVE (aiChecked=true)
├─── fail      → BLINDED (aiChecked=true, blindedReason="AI_DETECTED")
├─── need_review → ACTIVE + blindPending=true (관리자 큐 노출)
└─── 타임아웃/오류 → ACTIVE + blindPending=true (안전 fallback)

ACTIVE + reportCount >= 3 → 관리자 큐 노출 (status 변경 없음)

관리자 수동 블라인드 → BLINDED (blindedReason="ADMIN_REVIEWED", blindPending=false)
관리자 블라인드 해제 → ACTIVE (blindedReason=null, blindPending=false)

소비자 삭제 요청 → DELETED (soft delete)
```

---

## 2. Enum 설계

### `domain/src/main/kotlin/com/nearpick/domain/review/ReviewStatus.kt`

```kotlin
package com.nearpick.domain.review

enum class ReviewStatus {
    ACTIVE,    // 정상 노출
    BLINDED,   // AI 또는 관리자 블라인드
    DELETED,   // 소비자 삭제 (soft delete)
}
```

---

## 3. Entity 설계

### 3-1. `ReviewEntity`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/entity/ReviewEntity.kt`

```kotlin
package com.nearpick.nearpick.review.entity

import com.nearpick.domain.review.ReviewStatus
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.user.entity.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reviews",
    indexes = [
        Index(name = "idx_reviews_product_id", columnList = "product_id, status"),
        Index(name = "idx_reviews_user_id", columnList = "user_id"),
        Index(name = "idx_reviews_report_count", columnList = "report_count"),
    ]
)
class ReviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(name = "reservation_id", unique = true)
    val reservationId: Long? = null,

    @Column(name = "flash_purchase_id", unique = true)
    val flashPurchaseId: Long? = null,

    @Column(nullable = false)
    val rating: Int,

    @Column(nullable = false, length = 500)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReviewStatus = ReviewStatus.ACTIVE,

    @Column(name = "ai_checked", nullable = false)
    var aiChecked: Boolean = false,

    @Column(name = "ai_result", length = 20)
    var aiResult: String? = null,                // "pass" | "fail" | "need_review"

    @Column(name = "blinded_reason", length = 100)
    var blindedReason: String? = null,           // "AI_DETECTED" | "ADMIN_REVIEWED"

    @Column(name = "blind_pending", nullable = false)
    var blindPending: Boolean = false,            // need_review / fallback → 관리자 큐

    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

### 3-2. `ReviewImageEntity`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/entity/ReviewImageEntity.kt`

```kotlin
package com.nearpick.nearpick.review.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "review_images",
    indexes = [Index(name = "idx_review_images_review_id", columnList = "review_id")]
)
class ReviewImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    val review: ReviewEntity,

    @Column(name = "s3_key", nullable = false, length = 500)
    val s3Key: String,

    @Column(name = "image_url", nullable = false, length = 1000)
    val imageUrl: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
)
```

### 3-3. `ReviewReplyEntity`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/entity/ReviewReplyEntity.kt`

```kotlin
package com.nearpick.nearpick.review.entity

import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "review_replies")
class ReviewReplyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    val review: ReviewEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    val merchant: MerchantProfileEntity,

    @Column(nullable = false, length = 300)
    var content: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

---

## 4. Repository 설계

### 4-1. `ReviewRepository`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/repository/ReviewRepository.kt`

```kotlin
package com.nearpick.nearpick.review.repository

import com.nearpick.domain.review.ReviewStatus
import com.nearpick.nearpick.review.entity.ReviewEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<ReviewEntity, Long> {

    fun existsByReservationId(reservationId: Long): Boolean
    fun existsByFlashPurchaseId(flashPurchaseId: Long): Boolean

    // 상품 리뷰 목록 (PUBLIC) - ACTIVE만
    fun findByProductIdAndStatus(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<ReviewEntity>

    // 내 리뷰 목록 (DELETED 제외)
    fun findByUserIdAndStatusNot(
        userId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<ReviewEntity>

    // 평점 집계 쿼리
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    fun findAverageRatingByProductId(productId: Long): Double?

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    fun countActiveByProductId(productId: Long): Long

    // 관리자 큐: blindPending=true OR reportCount >= 3
    @Query("""
        SELECT r FROM ReviewEntity r
        WHERE r.blindPending = true OR r.reportCount >= :threshold
        ORDER BY r.reportCount DESC, r.createdAt DESC
    """)
    fun findAdminQueueReviews(threshold: Int = 3, pageable: Pageable): Page<ReviewEntity>
}
```

### 4-2. `ReviewImageRepository`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/repository/ReviewImageRepository.kt`

```kotlin
package com.nearpick.nearpick.review.repository

import com.nearpick.nearpick.review.entity.ReviewImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewImageRepository : JpaRepository<ReviewImageEntity, Long> {
    fun countByReviewId(reviewId: Long): Long
    fun findByReviewIdOrderByDisplayOrder(reviewId: Long): List<ReviewImageEntity>
}
```

### 4-3. `ReviewReplyRepository`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/repository/ReviewReplyRepository.kt`

```kotlin
package com.nearpick.nearpick.review.repository

import com.nearpick.nearpick.review.entity.ReviewReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReviewReplyRepository : JpaRepository<ReviewReplyEntity, Long> {
    fun existsByReviewId(reviewId: Long): Boolean
    fun findByReviewId(reviewId: Long): Optional<ReviewReplyEntity>
}
```

---

## 5. DTO 설계

**파일:** `domain/src/main/kotlin/com/nearpick/domain/review/dto/ReviewDtos.kt`

```kotlin
package com.nearpick.domain.review.dto

import com.nearpick.domain.review.ReviewStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ── Requests ──────────────────────────────────────────────────────────────────

data class ReviewCreateRequest(
    @field:NotNull val productId: Long,
    val reservationId: Long? = null,
    val flashPurchaseId: Long? = null,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(max = 500) val content: String,
)

data class ReviewReplyCreateRequest(
    @field:NotBlank @field:Size(max = 300) val content: String,
)

// ── Responses ─────────────────────────────────────────────────────────────────

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val images: List<ReviewImageItem>,
    val reply: ReviewReplyItem?,
    val reportCount: Int,
    val createdAt: LocalDateTime,
)

data class ReviewListItem(
    val id: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val images: List<ReviewImageItem>,
    val reply: ReviewReplyItem?,
    val createdAt: LocalDateTime,
)

data class ReviewImageItem(
    val id: Long,
    val imageUrl: String,
    val displayOrder: Int,
)

data class ReviewReplyItem(
    val id: Long,
    val merchantId: Long,
    val content: String,
    val createdAt: LocalDateTime,
)

// 리뷰 이미지 Presigned URL 응답
data class ReviewImageUploadResponse(
    val presignedUrl: String,
    val imageUrl: String,      // 업로드 후 저장할 공개 URL
    val s3Key: String,
)

// 관리자 큐 리뷰 응답 (blindPending, aiResult 포함)
data class AdminReviewItem(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val aiChecked: Boolean,
    val aiResult: String?,
    val blindedReason: String?,
    val blindPending: Boolean,
    val reportCount: Int,
    val images: List<ReviewImageItem>,
    val createdAt: LocalDateTime,
)
```

---

## 6. Domain 서비스 인터페이스 설계

### 6-1. `ReviewService`

**파일:** `domain/src/main/kotlin/com/nearpick/domain/review/ReviewService.kt`

```kotlin
package com.nearpick.domain.review

import com.nearpick.domain.review.dto.ReviewCreateRequest
import com.nearpick.domain.review.dto.ReviewListItem
import com.nearpick.domain.review.dto.ReviewResponse
import org.springframework.data.domain.Page

interface ReviewService {
    fun create(userId: Long, request: ReviewCreateRequest): ReviewResponse
    fun getProductReviews(productId: Long, page: Int, size: Int): Page<ReviewListItem>
    fun getMyReviews(userId: Long, page: Int, size: Int): Page<ReviewListItem>
    fun delete(userId: Long, reviewId: Long)
    fun report(userId: Long, reviewId: Long)
    fun adminBlind(reviewId: Long)
    fun adminUnblind(reviewId: Long)
    fun getAdminQueue(page: Int, size: Int): Page<com.nearpick.domain.review.dto.AdminReviewItem>
}
```

### 6-2. `ReviewReplyService`

**파일:** `domain/src/main/kotlin/com/nearpick/domain/review/ReviewReplyService.kt`

```kotlin
package com.nearpick.domain.review

import com.nearpick.domain.review.dto.ReviewReplyCreateRequest
import com.nearpick.domain.review.dto.ReviewResponse

interface ReviewReplyService {
    fun create(merchantId: Long, reviewId: Long, request: ReviewReplyCreateRequest): ReviewResponse
    fun delete(merchantId: Long, reviewId: Long)
}
```

### 6-3. `ReviewImageService`

**파일:** `domain/src/main/kotlin/com/nearpick/domain/review/ReviewImageService.kt`

```kotlin
package com.nearpick.domain.review

import com.nearpick.domain.review.dto.ReviewImageUploadResponse

interface ReviewImageService {
    fun getPresignedUrl(userId: Long, reviewId: Long, contentType: String): ReviewImageUploadResponse
    fun confirmUpload(userId: Long, reviewId: Long, s3Key: String, imageUrl: String)
}
```

### 6-4. `ReviewAiService`

**파일:** `domain/src/main/kotlin/com/nearpick/domain/review/ReviewAiService.kt`

```kotlin
package com.nearpick.domain.review

interface ReviewAiService {
    /** 비동기로 reviewId에 대한 AI 검증 실행. @Async 구현체에서 처리. */
    fun checkAsync(reviewId: Long)
}
```

---

## 7. AI 서비스 구현 설계 (Claude API)

### `ReviewAiServiceImpl`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/service/ReviewAiServiceImpl.kt`

```kotlin
@Service
@Profile("!test")
class ReviewAiServiceImpl(
    @Value("\${anthropic.api-key}") private val apiKey: String,
    private val reviewRepository: ReviewRepository,
    private val productRepository: ProductRepository,
) : ReviewAiService {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .defaultHeader("x-api-key", apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("Content-Type", "application/json")
        .build()

    @Async("reviewAiExecutor")
    override fun checkAsync(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElse(null) ?: return
        val product = review.product

        try {
            val result = callClaude(
                productTitle = product.title,
                category = product.category?.name ?: "기타",
                reviewContent = review.content,
                ocrText = "(없음)",  // 이미지 OCR은 별도 비동기 흐름 (현재 구현 외)
            )
            when (result.result) {
                "pass" -> {
                    review.aiChecked = true
                    review.aiResult = "pass"
                }
                "fail" -> {
                    review.aiChecked = true
                    review.aiResult = "fail"
                    review.status = ReviewStatus.BLINDED
                    review.blindedReason = "AI_DETECTED"
                }
                "need_review" -> {
                    review.aiChecked = true
                    review.aiResult = "need_review"
                    review.blindPending = true
                }
                else -> {
                    review.blindPending = true  // 예외 응답 → 안전 fallback
                }
            }
            reviewRepository.save(review)
        } catch (e: Exception) {
            // 타임아웃 / 네트워크 오류 → blindPending fallback
            review.blindPending = true
            reviewRepository.save(review)
        }
    }

    private fun callClaude(
        productTitle: String,
        category: String,
        reviewContent: String,
        ocrText: String,
    ): AiReviewResult {
        val systemPrompt = """
            너는 전자상거래 플랫폼 리뷰 정책 심사관이다.
            제공된 상품명·카테고리·리뷰 텍스트·OCR 텍스트만 사용해 판단한다.
            외부 지식이나 추론은 사용하지 않는다.

            판단 기준:
            - fail       : 비속어/욕설, 특정인 비방, 개인정보 노출, 광고성 스팸, 무의미 반복 텍스트
            - pass       : 그 외 정상 리뷰
            - need_review: 경계선 사례 또는 맥락이 불분명한 경우

            반드시 아래 JSON만 출력한다. 다른 텍스트는 절대 포함하지 않는다:
            {"result":"pass"|"fail"|"need_review","reason":"fail·need_review 시 한 문장 필수, pass 시 null"}
        """.trimIndent()

        val userPrompt = """
            상품명: $productTitle
            카테고리: $category
            리뷰: $reviewContent
            OCR: $ocrText
        """.trimIndent()

        val requestBody = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 200,
            "system" to systemPrompt,
            "messages" to listOf(mapOf("role" to "user", "content" to userPrompt)),
        )

        val response = restClient.post()
            .uri("/v1/messages")
            .body(requestBody)
            .retrieve()
            .body(ClaudeApiResponse::class.java)
            ?: throw RuntimeException("Claude API returned null")

        val jsonText = response.content.first().text
        return objectMapper.readValue(jsonText, AiReviewResult::class.java)
    }
}

data class AiReviewResult(val result: String, val reason: String?)
data class ClaudeApiResponse(val content: List<ClaudeContent>)
data class ClaudeContent(val text: String)
```

### `NoOpReviewAiServiceImpl` (test profile)

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/service/NoOpReviewAiServiceImpl.kt`

```kotlin
@Service
@Profile("test")
class NoOpReviewAiServiceImpl : ReviewAiService {
    override fun checkAsync(reviewId: Long) { /* no-op */ }
}
```

### Thread Pool 설정

**파일:** `app/src/main/kotlin/com/nearpick/app/config/AsyncConfig.kt`

```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean("reviewAiExecutor")
    fun reviewAiExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 5
            queueCapacity = 100
            setThreadNamePrefix("review-ai-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(5)
            initialize()
        }
    }
}
```

---

## 8. ServiceImpl 설계

### 8-1. `ReviewServiceImpl`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/service/ReviewServiceImpl.kt`

```kotlin
@Service
@Transactional
class ReviewServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val reviewReplyRepository: ReviewReplyRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val reservationRepository: ReservationRepository,
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val reviewAiService: ReviewAiService,
) : ReviewService {

    override fun create(userId: Long, request: ReviewCreateRequest): ReviewResponse {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = productRepository.findById(request.productId).orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }

        // 자격 검증
        when {
            request.reservationId != null -> {
                val reservation = reservationRepository.findById(request.reservationId)
                    .orElseThrow { BusinessException(ErrorCode.RESERVATION_NOT_FOUND) }
                if (reservation.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
                if (reservation.status != ReservationStatus.COMPLETED) throw BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE)
                if (reviewRepository.existsByReservationId(request.reservationId)) throw BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS)
            }
            request.flashPurchaseId != null -> {
                val fp = flashPurchaseRepository.findById(request.flashPurchaseId)
                    .orElseThrow { BusinessException(ErrorCode.FLASH_PURCHASE_NOT_FOUND) }
                if (fp.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
                if (fp.status != FlashPurchaseStatus.PICKED_UP) throw BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE)
                if (reviewRepository.existsByFlashPurchaseId(request.flashPurchaseId)) throw BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS)
            }
            else -> throw BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE)
        }

        val review = reviewRepository.save(
            ReviewEntity(
                user = user,
                product = product,
                reservationId = request.reservationId,
                flashPurchaseId = request.flashPurchaseId,
                rating = request.rating,
                content = request.content,
            )
        )

        // 평점 집계 갱신
        updateProductRating(product.id)

        // AI 검증 비동기 실행
        reviewAiService.checkAsync(review.id)

        return review.toResponse(images = emptyList(), reply = null)
    }

    override fun delete(userId: Long, reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        review.status = ReviewStatus.DELETED
        updateProductRating(review.product.id)
    }

    override fun report(userId: Long, reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.reportCount++
    }

    override fun adminBlind(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.status = ReviewStatus.BLINDED
        review.blindedReason = "ADMIN_REVIEWED"
        review.blindPending = false
        updateProductRating(review.product.id)
    }

    override fun adminUnblind(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.status = ReviewStatus.ACTIVE
        review.blindedReason = null
        review.blindPending = false
        updateProductRating(review.product.id)
    }

    private fun updateProductRating(productId: Long) {
        val product = productRepository.findById(productId).orElse(null) ?: return
        val avg = reviewRepository.findAverageRatingByProductId(productId) ?: 0.0
        val count = reviewRepository.countActiveByProductId(productId)
        product.averageRating = BigDecimal(avg).setScale(2, RoundingMode.HALF_UP)
        product.reviewCount = count.toInt()
    }
}
```

### 8-2. `ReviewReplyServiceImpl`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/service/ReviewReplyServiceImpl.kt`

```kotlin
@Service
@Transactional
class ReviewReplyServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val reviewReplyRepository: ReviewReplyRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
) : ReviewReplyService {

    override fun create(merchantId: Long, reviewId: Long, request: ReviewReplyCreateRequest): ReviewResponse {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        if (reviewReplyRepository.existsByReviewId(reviewId)) throw BusinessException(ErrorCode.REVIEW_REPLY_ALREADY_EXISTS)

        val merchant = merchantProfileRepository.findById(merchantId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        reviewReplyRepository.save(ReviewReplyEntity(review = review, merchant = merchant, content = request.content))

        return review.toResponse(...)
    }

    override fun delete(merchantId: Long, reviewId: Long) {
        val reply = reviewReplyRepository.findByReviewId(reviewId)
            .orElseThrow { BusinessException(ErrorCode.REVIEW_REPLY_NOT_FOUND) }
        if (reply.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        reviewReplyRepository.delete(reply)
    }
}
```

### 8-3. `ReviewImageServiceImpl`

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/service/ReviewImageServiceImpl.kt`

```kotlin
@Service
@Transactional
class ReviewImageServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val imageStorageService: ImageStorageService,
) : ReviewImageService {

    override fun getPresignedUrl(userId: Long, reviewId: Long, contentType: String): ReviewImageUploadResponse {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        val count = reviewImageRepository.countByReviewId(reviewId)
        if (count >= 3) throw BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED)

        val s3Key = "review-images/${reviewId}/${UUID.randomUUID()}"
        val presignedUrl = imageStorageService.generatePresignedPutUrl(s3Key, contentType)
        val imageUrl = imageStorageService.buildPublicUrl(s3Key)
        return ReviewImageUploadResponse(presignedUrl = presignedUrl, imageUrl = imageUrl, s3Key = s3Key)
    }

    override fun confirmUpload(userId: Long, reviewId: Long, s3Key: String, imageUrl: String) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        val count = reviewImageRepository.countByReviewId(reviewId)
        if (count >= 3) throw BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED)
        reviewImageRepository.save(
            ReviewImageEntity(
                review = review,
                s3Key = s3Key,
                imageUrl = imageUrl,
                displayOrder = count.toInt(),
            )
        )
    }
}
```

---

## 9. ProductEntity 변경

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/entity/ProductEntity.kt`

```kotlin
// 기존 필드 아래에 추가
@Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
var averageRating: BigDecimal = BigDecimal.ZERO,

@Column(name = "review_count", nullable = false)
var reviewCount: Int = 0,
```

---

## 10. ErrorCode 추가

**파일:** `common/src/main/kotlin/com/nearpick/common/exception/ErrorCode.kt`

```kotlin
// Review System (Phase 13)
REVIEW_NOT_FOUND(404, "리뷰를 찾을 수 없습니다."),
REVIEW_ALREADY_EXISTS(422, "이미 해당 거래에 대한 리뷰가 존재합니다."),
REVIEW_NOT_ELIGIBLE(422, "구매 또는 방문 완료 후에만 리뷰를 작성할 수 있습니다."),
REVIEW_BLINDED(403, "블라인드 처리된 리뷰입니다."),
REVIEW_REPLY_ALREADY_EXISTS(422, "이미 답글이 존재합니다."),
REVIEW_REPLY_NOT_FOUND(404, "답글을 찾을 수 없습니다."),
REVIEW_IMAGE_LIMIT_EXCEEDED(422, "리뷰 이미지는 최대 3장까지 등록 가능합니다."),
```

---

## 11. Controller 설계

### 11-1. `ReviewController`

**파일:** `app/src/main/kotlin/com/nearpick/app/controller/ReviewController.kt`

| 메서드 | 엔드포인트 | 인증 | 설명 |
|--------|-----------|------|------|
| POST | `/api/reviews` | CONSUMER | 리뷰 작성 |
| GET | `/api/reviews/product/{productId}` | PUBLIC | 상품 리뷰 목록 |
| GET | `/api/reviews/me` | CONSUMER | 내 리뷰 목록 |
| DELETE | `/api/reviews/{reviewId}` | CONSUMER | 리뷰 삭제 |
| POST | `/api/reviews/{reviewId}/images` | CONSUMER | Presigned URL 발급 |
| POST | `/api/reviews/{reviewId}/images/confirm` | CONSUMER | 업로드 완료 확인 |
| POST | `/api/reviews/{reviewId}/reply` | MERCHANT | 소상공인 답글 작성 |
| DELETE | `/api/reviews/{reviewId}/reply` | MERCHANT | 소상공인 답글 삭제 |
| POST | `/api/reviews/{reviewId}/report` | CONSUMER | 리뷰 신고 |

```kotlin
@RestController
@RequestMapping("/api/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val reviewReplyService: ReviewReplyService,
    private val reviewImageService: ReviewImageService,
) {
    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReviewCreateRequest,
    ): ResponseEntity<ApiResponse<ReviewResponse>> {
        return ResponseEntity.status(201).body(ApiResponse.success(reviewService.create(userId, request)))
    }

    @GetMapping("/product/{productId}")
    fun getProductReviews(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ApiResponse<Page<ReviewListItem>>> {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductReviews(productId, page, size)))
    }

    @GetMapping("/me")
    fun getMyReviews(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<ApiResponse<Page<ReviewListItem>>> {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getMyReviews(userId, page, size)))
    }

    @DeleteMapping("/{reviewId}")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
    ): ResponseEntity<ApiResponse<Void>> {
        reviewService.delete(userId, reviewId)
        return ResponseEntity.ok(ApiResponse.success(null))
    }

    @PostMapping("/{reviewId}/images")
    fun getPresignedUrl(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
        @RequestParam contentType: String,
    ): ResponseEntity<ApiResponse<ReviewImageUploadResponse>> {
        return ResponseEntity.ok(ApiResponse.success(reviewImageService.getPresignedUrl(userId, reviewId, contentType)))
    }

    @PostMapping("/{reviewId}/images/confirm")
    fun confirmImageUpload(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
        @RequestParam s3Key: String,
        @RequestParam imageUrl: String,
    ): ResponseEntity<ApiResponse<Void>> {
        reviewImageService.confirmUpload(userId, reviewId, s3Key, imageUrl)
        return ResponseEntity.ok(ApiResponse.success(null))
    }

    @PostMapping("/{reviewId}/reply")
    fun createReply(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable reviewId: Long,
        @RequestBody @Valid request: ReviewReplyCreateRequest,
    ): ResponseEntity<ApiResponse<ReviewResponse>> {
        return ResponseEntity.status(201).body(ApiResponse.success(reviewReplyService.create(merchantId, reviewId, request)))
    }

    @DeleteMapping("/{reviewId}/reply")
    fun deleteReply(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable reviewId: Long,
    ): ResponseEntity<ApiResponse<Void>> {
        reviewReplyService.delete(merchantId, reviewId)
        return ResponseEntity.ok(ApiResponse.success(null))
    }

    @PostMapping("/{reviewId}/report")
    fun report(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
    ): ResponseEntity<ApiResponse<Void>> {
        reviewService.report(userId, reviewId)
        return ResponseEntity.ok(ApiResponse.success(null))
    }
}
```

### 11-2. `AdminReviewController`

**파일:** `app/src/main/kotlin/com/nearpick/app/controller/AdminReviewController.kt`

| 메서드 | 엔드포인트 | 인증 | 설명 |
|--------|-----------|------|------|
| GET | `/api/admin/reviews` | ADMIN | 관리자 리뷰 큐 목록 |
| PATCH | `/api/admin/reviews/{reviewId}/blind` | ADMIN | 수동 블라인드 |
| PATCH | `/api/admin/reviews/{reviewId}/unblind` | ADMIN | 블라인드 해제 |

```kotlin
@RestController
@RequestMapping("/api/admin/reviews")
class AdminReviewController(
    private val reviewService: ReviewService,
) {
    @GetMapping
    fun getAdminQueue(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<AdminReviewItem>>> {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAdminQueue(page, size)))
    }

    @PatchMapping("/{reviewId}/blind")
    fun blind(@PathVariable reviewId: Long): ResponseEntity<ApiResponse<Void>> {
        reviewService.adminBlind(reviewId)
        return ResponseEntity.ok(ApiResponse.success(null))
    }

    @PatchMapping("/{reviewId}/unblind")
    fun unblind(@PathVariable reviewId: Long): ResponseEntity<ApiResponse<Void>> {
        reviewService.adminUnblind(reviewId)
        return ResponseEntity.ok(ApiResponse.success(null))
    }
}
```

---

## 12. Flyway V7 마이그레이션

**파일:** `app/src/main/resources/db/migration/V7__review_system.sql`

```sql
-- reviews
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    reservation_id BIGINT NULL UNIQUE,
    flash_purchase_id BIGINT NULL UNIQUE,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ai_checked BOOLEAN NOT NULL DEFAULT FALSE,
    ai_result VARCHAR(20) NULL,
    blinded_reason VARCHAR(100) NULL,
    blind_pending BOOLEAN NOT NULL DEFAULT FALSE,
    report_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- review_images
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (review_id) REFERENCES reviews(id)
);

-- review_replies
CREATE TABLE review_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    content VARCHAR(300) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(user_id)
);

-- products 평점 집계 필드 추가
ALTER TABLE products
    ADD COLUMN average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

-- 인덱스
CREATE INDEX idx_reviews_product_id ON reviews (product_id, status);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_reviews_report_count ON reviews (report_count);
CREATE INDEX idx_review_images_review_id ON review_images (review_id);
```

---

## 13. application.properties 추가

**파일:** `app/src/main/resources/application.properties`

```properties
# AI (Claude API)
anthropic.api-key=${ANTHROPIC_API_KEY:}
```

---

## 14. Mapper 설계

**파일:** `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/review/mapper/ReviewMapper.kt`

```kotlin
package com.nearpick.nearpick.review.mapper

import com.nearpick.domain.review.dto.*
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.entity.ReviewImageEntity
import com.nearpick.nearpick.review.entity.ReviewReplyEntity

object ReviewMapper {

    fun toResponse(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
        reply: ReviewReplyEntity?,
    ): ReviewResponse = ReviewResponse(
        id = review.id,
        productId = review.product.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        images = images.map { toImageItem(it) },
        reply = reply?.let { toReplyItem(it) },
        reportCount = review.reportCount,
        createdAt = review.createdAt,
    )

    fun toListItem(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
        reply: ReviewReplyEntity?,
    ): ReviewListItem = ReviewListItem(
        id = review.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        images = images.map { toImageItem(it) },
        reply = reply?.let { toReplyItem(it) },
        createdAt = review.createdAt,
    )

    fun toAdminItem(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
    ): AdminReviewItem = AdminReviewItem(
        id = review.id,
        productId = review.product.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        aiChecked = review.aiChecked,
        aiResult = review.aiResult,
        blindedReason = review.blindedReason,
        blindPending = review.blindPending,
        reportCount = review.reportCount,
        images = images.map { toImageItem(it) },
        createdAt = review.createdAt,
    )

    private fun toImageItem(image: ReviewImageEntity): ReviewImageItem = ReviewImageItem(
        id = image.id,
        imageUrl = image.imageUrl,
        displayOrder = image.displayOrder,
    )

    private fun toReplyItem(reply: ReviewReplyEntity): ReviewReplyItem = ReviewReplyItem(
        id = reply.id,
        merchantId = reply.merchant.userId,
        content = reply.content,
        createdAt = reply.createdAt,
    )
}
```

---

## 15. 테스트 설계

**파일:** `domain-nearpick/src/test/kotlin/com/nearpick/nearpick/review/service/ReviewServiceImplTest.kt`

### 테스트 케이스

| # | 테스트 | 검증 포인트 |
|---|--------|-----------|
| 1 | `create - Reservation COMPLETED → 리뷰 저장 성공` | ReviewResponse 반환, aiChecked=false |
| 2 | `create - FlashPurchase PICKED_UP → 리뷰 저장 성공` | ReviewResponse 반환 |
| 3 | `create - PENDING 예약에 리뷰 작성 시 REVIEW_NOT_ELIGIBLE` | 예외 코드 검증 |
| 4 | `create - 중복 리뷰 작성 시 REVIEW_ALREADY_EXISTS` | existsByReservationId → true |
| 5 | `create - 타인 예약에 리뷰 작성 시 FORBIDDEN` | 예외 코드 검증 |
| 6 | `delete - 본인 리뷰 삭제 → DELETED 상태` | review.status == DELETED |
| 7 | `delete - 타인 리뷰 삭제 시 FORBIDDEN` | 예외 코드 검증 |
| 8 | `report - 신고 횟수 +1` | review.reportCount 증가 |
| 9 | `adminBlind - 블라인드 처리 → BLINDED, blindedReason=ADMIN_REVIEWED` | 상태 검증 |
| 10 | `adminUnblind - 해제 → ACTIVE, blindedReason=null` | 상태 검증 |
| 11 | `updateProductRating - ACTIVE 리뷰 기준 평점 집계` | product.averageRating 갱신 |

**파일:** `domain-nearpick/src/test/kotlin/com/nearpick/nearpick/review/service/ReviewReplyServiceImplTest.kt`

| # | 테스트 | 검증 포인트 |
|---|--------|-----------|
| 1 | `create - 상품 소유 소상공인이 답글 작성 → 성공` | ReviewReplyEntity 저장 |
| 2 | `create - 다른 소상공인이 답글 작성 시 FORBIDDEN` | 예외 코드 검증 |
| 3 | `create - 중복 답글 시 REVIEW_REPLY_ALREADY_EXISTS` | 예외 코드 검증 |
| 4 | `delete - 본인 답글 삭제 → 성공` | delete 호출 검증 |

---

## 16. 구현 순서 (Implementation Order)

```
1. domain/review/ReviewStatus.kt                 (enum)
2. common/exception/ErrorCode.kt                 (7개 추가)
3. domain-nearpick/review/entity/ReviewEntity.kt
4. domain-nearpick/review/entity/ReviewImageEntity.kt
5. domain-nearpick/review/entity/ReviewReplyEntity.kt
6. app/src/main/resources/db/migration/V7__review_system.sql
7. domain-nearpick/product/entity/ProductEntity.kt  (averageRating, reviewCount 필드 추가)
8. domain-nearpick/review/repository/ReviewRepository.kt
9. domain-nearpick/review/repository/ReviewImageRepository.kt
10. domain-nearpick/review/repository/ReviewReplyRepository.kt
11. domain/review/dto/ReviewDtos.kt
12. domain/review/ReviewService.kt
13. domain/review/ReviewReplyService.kt
14. domain/review/ReviewImageService.kt
15. domain/review/ReviewAiService.kt
16. domain-nearpick/review/mapper/ReviewMapper.kt
17. domain-nearpick/review/service/ReviewAiServiceImpl.kt  (Claude API)
18. domain-nearpick/review/service/NoOpReviewAiServiceImpl.kt  (test profile)
19. app/config/AsyncConfig.kt  (@EnableAsync + reviewAiExecutor bean)
20. domain-nearpick/review/service/ReviewServiceImpl.kt
21. domain-nearpick/review/service/ReviewReplyServiceImpl.kt
22. domain-nearpick/review/service/ReviewImageServiceImpl.kt
23. app/controller/ReviewController.kt
24. app/controller/AdminReviewController.kt
25. application.properties (anthropic.api-key 추가)
26. 테스트 작성 (ReviewServiceImplTest, ReviewReplyServiceImplTest)
```

---

## 17. 변경 파일 목록

### 신규 파일 (25개)

| 파일 | 모듈 |
|------|------|
| `domain/review/ReviewStatus.kt` | domain |
| `domain/review/ReviewService.kt` | domain |
| `domain/review/ReviewReplyService.kt` | domain |
| `domain/review/ReviewImageService.kt` | domain |
| `domain/review/ReviewAiService.kt` | domain |
| `domain/review/dto/ReviewDtos.kt` | domain |
| `domain-nearpick/review/entity/ReviewEntity.kt` | domain-nearpick |
| `domain-nearpick/review/entity/ReviewImageEntity.kt` | domain-nearpick |
| `domain-nearpick/review/entity/ReviewReplyEntity.kt` | domain-nearpick |
| `domain-nearpick/review/repository/ReviewRepository.kt` | domain-nearpick |
| `domain-nearpick/review/repository/ReviewImageRepository.kt` | domain-nearpick |
| `domain-nearpick/review/repository/ReviewReplyRepository.kt` | domain-nearpick |
| `domain-nearpick/review/mapper/ReviewMapper.kt` | domain-nearpick |
| `domain-nearpick/review/service/ReviewServiceImpl.kt` | domain-nearpick |
| `domain-nearpick/review/service/ReviewReplyServiceImpl.kt` | domain-nearpick |
| `domain-nearpick/review/service/ReviewImageServiceImpl.kt` | domain-nearpick |
| `domain-nearpick/review/service/ReviewAiServiceImpl.kt` | domain-nearpick |
| `domain-nearpick/review/service/NoOpReviewAiServiceImpl.kt` | domain-nearpick |
| `app/controller/ReviewController.kt` | app |
| `app/controller/AdminReviewController.kt` | app |
| `app/config/AsyncConfig.kt` | app |
| `app/db/migration/V7__review_system.sql` | app |
| `domain-nearpick/test/.../review/service/ReviewServiceImplTest.kt` | domain-nearpick test |
| `domain-nearpick/test/.../review/service/ReviewReplyServiceImplTest.kt` | domain-nearpick test |

### 변경 파일 (3개)

| 파일 | 변경 내용 |
|------|----------|
| `common/exception/ErrorCode.kt` | 7개 ErrorCode 추가 |
| `domain-nearpick/product/entity/ProductEntity.kt` | `averageRating`, `reviewCount` 필드 추가 |
| `app/resources/application.properties` | `anthropic.api-key` 추가 |
