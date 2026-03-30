package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.review.ReviewAiService
import com.nearpick.domain.review.ReviewStatus
import com.nearpick.domain.review.dto.ReviewCreateRequest
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewReplyRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.entity.ReservationEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReviewServiceImplTest {

    @Mock lateinit var reviewRepository: ReviewRepository
    @Mock lateinit var reviewImageRepository: ReviewImageRepository
    @Mock lateinit var reviewReplyRepository: ReviewReplyRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var reviewAiService: ReviewAiService

    @InjectMocks lateinit var reviewService: ReviewServiceImpl

    private lateinit var consumer: UserEntity
    private lateinit var otherUser: UserEntity
    private lateinit var merchantUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity
    private lateinit var completedReservation: ReservationEntity
    private lateinit var pickedUpFlashPurchase: FlashPurchaseEntity

    @BeforeEach
    fun setUp() {
        consumer = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        otherUser = UserEntity(id = 99L, email = "other@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        product = ProductEntity(
            id = 10L, merchant = merchant, title = "테스트 상품",
            price = 5000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
            category = ProductCategory.FOOD,
        )
        completedReservation = ReservationEntity(
            id = 1L, user = consumer, product = product, quantity = 1,
            status = ReservationStatus.COMPLETED,
        )
        pickedUpFlashPurchase = FlashPurchaseEntity(
            id = 2L, user = consumer, product = product, quantity = 1,
            status = FlashPurchaseStatus.PICKED_UP,
        )
    }

    private fun savedReview(id: Long = 100L) = ReviewEntity(
        id = id, user = consumer, product = product,
        rating = 5, content = "좋아요", reservationId = 1L,
    )

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    fun `create - Reservation COMPLETED 리뷰 작성 성공`() {
        val request = ReviewCreateRequest(productId = 10L, reservationId = 1L, rating = 5, content = "좋아요")
        val review = savedReview()

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(completedReservation))
        whenever(reviewRepository.existsByReservationId(1L)).thenReturn(false)
        whenever(reviewRepository.save(any())).thenReturn(review)
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(5.0)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(1L)

        val response = reviewService.create(1L, request)

        assertEquals(100L, response.id)
        assertEquals(ReviewStatus.ACTIVE, response.status)
    }

    @Test
    fun `create - FlashPurchase PICKED_UP 리뷰 작성 성공`() {
        val request = ReviewCreateRequest(productId = 10L, flashPurchaseId = 2L, rating = 4, content = "좋습니다")
        val review = ReviewEntity(id = 101L, user = consumer, product = product, rating = 4, content = "좋습니다", flashPurchaseId = 2L)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(flashPurchaseRepository.findById(2L)).thenReturn(Optional.of(pickedUpFlashPurchase))
        whenever(reviewRepository.existsByFlashPurchaseId(2L)).thenReturn(false)
        whenever(reviewRepository.save(any())).thenReturn(review)
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(4.0)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(1L)

        val response = reviewService.create(1L, request)

        assertEquals(101L, response.id)
    }

    @Test
    fun `create - PENDING 예약에 리뷰 작성 시 REVIEW_NOT_ELIGIBLE`() {
        val pendingReservation = ReservationEntity(id = 1L, user = consumer, product = product, quantity = 1, status = ReservationStatus.PENDING)
        val request = ReviewCreateRequest(productId = 10L, reservationId = 1L, rating = 5, content = "테스트")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation))

        val ex = assertThrows<BusinessException> { reviewService.create(1L, request) }
        assertEquals(ErrorCode.REVIEW_NOT_ELIGIBLE, ex.errorCode)
    }

    @Test
    fun `create - 중복 리뷰 작성 시 REVIEW_ALREADY_EXISTS`() {
        val request = ReviewCreateRequest(productId = 10L, reservationId = 1L, rating = 5, content = "테스트")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(completedReservation))
        whenever(reviewRepository.existsByReservationId(1L)).thenReturn(true)

        val ex = assertThrows<BusinessException> { reviewService.create(1L, request) }
        assertEquals(ErrorCode.REVIEW_ALREADY_EXISTS, ex.errorCode)
    }

    @Test
    fun `create - 타인 예약에 리뷰 작성 시 FORBIDDEN`() {
        val otherReservation = ReservationEntity(id = 1L, user = otherUser, product = product, quantity = 1, status = ReservationStatus.COMPLETED)
        val request = ReviewCreateRequest(productId = 10L, reservationId = 1L, rating = 5, content = "테스트")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(otherReservation))

        val ex = assertThrows<BusinessException> { reviewService.create(1L, request) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    fun `delete - 본인 리뷰 삭제 시 DELETED 상태로 변경`() {
        val review = savedReview()

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(null)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(0L)

        reviewService.delete(1L, 100L)

        assertEquals(ReviewStatus.DELETED, review.status)
    }

    @Test
    fun `delete - 타인 리뷰 삭제 시 FORBIDDEN`() {
        val review = savedReview()
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))

        val ex = assertThrows<BusinessException> { reviewService.delete(99L, 100L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    // ── report ─────────────────────────────────────────────────────────────────

    @Test
    fun `report - 신고 시 reportCount 1 증가`() {
        val review = savedReview()
        val initialCount = review.reportCount
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))

        reviewService.report(1L, 100L)

        assertEquals(initialCount + 1, review.reportCount)
    }

    // ── adminBlind / adminUnblind ───────────────────────────────────────────────

    @Test
    fun `adminBlind - 블라인드 처리 시 BLINDED 상태와 사유 설정`() {
        val review = savedReview()

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(null)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(0L)

        reviewService.adminBlind(100L)

        assertEquals(ReviewStatus.BLINDED, review.status)
        assertEquals("ADMIN_REVIEWED", review.blindedReason)
        assertEquals(false, review.blindPending)
    }

    @Test
    fun `adminUnblind - 해제 시 ACTIVE 상태와 blindedReason null`() {
        val review = ReviewEntity(id = 100L, user = consumer, product = product, rating = 5, content = "좋아요", reservationId = 1L).apply {
            status = ReviewStatus.BLINDED
            blindedReason = "ADMIN_REVIEWED"
        }

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(5.0)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(1L)

        reviewService.adminUnblind(100L)

        assertEquals(ReviewStatus.ACTIVE, review.status)
        assertEquals(null, review.blindedReason)
    }

    // ── create - 자격 없음 (reservationId/flashPurchaseId 모두 null) ──────────────

    @Test
    fun `create - reservationId와 flashPurchaseId 모두 null이면 REVIEW_NOT_ELIGIBLE`() {
        val request = ReviewCreateRequest(productId = 10L, rating = 5, content = "자격 없음")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> { reviewService.create(1L, request) }
        assertEquals(ErrorCode.REVIEW_NOT_ELIGIBLE, ex.errorCode)
    }

    // ── getProductReviews ───────────────────────────────────────────────────────

    @Test
    fun `getProductReviews - ACTIVE 리뷰 목록 반환`() {
        val review = savedReview()
        val page = PageImpl(listOf(review), PageRequest.of(0, 10), 1)

        whenever(reviewRepository.findByProductIdAndStatus(10L, ReviewStatus.ACTIVE, PageRequest.of(0, 10))).thenReturn(page)
        whenever(reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)).thenReturn(emptyList())
        whenever(reviewReplyRepository.findByReviewId(review.id)).thenReturn(Optional.empty())

        val result = reviewService.getProductReviews(10L, 0, 10)

        assertEquals(1, result.totalElements)
        assertEquals(5, result.content.first().rating)
    }

    // ── getMyReviews ────────────────────────────────────────────────────────────

    @Test
    fun `getMyReviews - DELETED 제외한 내 리뷰 목록 반환`() {
        val review = savedReview()
        val page = PageImpl(listOf(review), PageRequest.of(0, 10), 1)

        whenever(reviewRepository.findByUserIdAndStatusNot(1L, ReviewStatus.DELETED, PageRequest.of(0, 10))).thenReturn(page)
        whenever(reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)).thenReturn(emptyList())
        whenever(reviewReplyRepository.findByReviewId(review.id)).thenReturn(Optional.empty())

        val result = reviewService.getMyReviews(1L, 0, 10)

        assertEquals(1, result.totalElements)
    }

    // ── getAdminQueue ───────────────────────────────────────────────────────────

    @Test
    fun `getAdminQueue - blindPending 리뷰 목록 반환`() {
        val review = savedReview().apply { blindPending = true }
        val page = PageImpl(listOf(review), PageRequest.of(0, 20), 1)

        whenever(reviewRepository.findAdminQueueReviews(pageable = PageRequest.of(0, 20))).thenReturn(page)
        whenever(reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)).thenReturn(emptyList())

        val result = reviewService.getAdminQueue(0, 20)

        assertEquals(1, result.totalElements)
        assertEquals(true, result.content.first().blindPending)
    }

    // ── updateProductRating ─────────────────────────────────────────────────────

    @Test
    fun `create 후 updateProductRating - ACTIVE 리뷰 기준 평점 집계 갱신`() {
        val request = ReviewCreateRequest(productId = 10L, reservationId = 1L, rating = 4, content = "평균 테스트")
        val review = savedReview()

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(completedReservation))
        whenever(reviewRepository.existsByReservationId(1L)).thenReturn(false)
        whenever(reviewRepository.save(any())).thenReturn(review)
        whenever(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(3.5)
        whenever(reviewRepository.countActiveByProductId(10L)).thenReturn(2L)

        reviewService.create(1L, request)

        assertEquals(BigDecimal("3.50"), product.averageRating)
        assertEquals(2, product.reviewCount)
    }
}
