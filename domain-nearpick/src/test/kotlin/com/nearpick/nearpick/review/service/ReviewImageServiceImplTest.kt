package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ImageStorageService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReviewImageServiceImplTest {

    @Mock lateinit var reviewRepository: ReviewRepository
    @Mock lateinit var reviewImageRepository: ReviewImageRepository
    @Mock lateinit var imageStorageService: ImageStorageService

    @InjectMocks lateinit var reviewImageService: ReviewImageServiceImpl

    private lateinit var consumer: UserEntity
    private lateinit var otherUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity
    private lateinit var review: ReviewEntity

    @BeforeEach
    fun setUp() {
        consumer = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        otherUser = UserEntity(id = 99L, email = "other@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        val merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
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
        )
        review = ReviewEntity(id = 100L, user = consumer, product = product, rating = 5, content = "좋아요", reservationId = 1L)
    }

    // ── getPresignedUrl ─────────────────────────────────────────────────────────

    @Test
    fun `getPresignedUrl - 성공 시 Presigned URL 반환`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewImageRepository.countByReviewId(100L)).thenReturn(0L)
        whenever(imageStorageService.generatePresignedPutUrl(any(), any())).thenReturn("https://s3.example.com/presigned")
        whenever(imageStorageService.buildPublicUrl(any())).thenReturn("https://cdn.example.com/image.jpg")

        val response = reviewImageService.getPresignedUrl(1L, 100L, "image/jpeg")

        assertEquals("https://s3.example.com/presigned", response.presignedUrl)
        assertEquals("https://cdn.example.com/image.jpg", response.imageUrl)
    }

    @Test
    fun `getPresignedUrl - 리뷰 미존재 시 REVIEW_NOT_FOUND`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { reviewImageService.getPresignedUrl(1L, 100L, "image/jpeg") }
        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `getPresignedUrl - 타인 리뷰에 요청 시 FORBIDDEN`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))

        val ex = assertThrows<BusinessException> { reviewImageService.getPresignedUrl(99L, 100L, "image/jpeg") }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `getPresignedUrl - 이미지 3장 초과 시 REVIEW_IMAGE_LIMIT_EXCEEDED`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewImageRepository.countByReviewId(100L)).thenReturn(3L)

        val ex = assertThrows<BusinessException> { reviewImageService.getPresignedUrl(1L, 100L, "image/jpeg") }
        assertEquals(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED, ex.errorCode)
    }

    // ── confirmUpload ───────────────────────────────────────────────────────────

    @Test
    fun `confirmUpload - 성공 시 ReviewImageEntity 저장`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewImageRepository.countByReviewId(100L)).thenReturn(1L)

        reviewImageService.confirmUpload(1L, 100L, "review-images/100/uuid", "https://cdn.example.com/image.jpg")

        verify(reviewImageRepository).save(any())
    }

    @Test
    fun `confirmUpload - 이미지 3장 초과 시 REVIEW_IMAGE_LIMIT_EXCEEDED`() {
        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewImageRepository.countByReviewId(100L)).thenReturn(3L)

        val ex = assertThrows<BusinessException> {
            reviewImageService.confirmUpload(1L, 100L, "review-images/100/uuid", "https://cdn.example.com/image.jpg")
        }
        assertEquals(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED, ex.errorCode)
    }
}
