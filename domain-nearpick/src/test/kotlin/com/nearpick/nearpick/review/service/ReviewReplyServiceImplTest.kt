package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.review.dto.ReviewReplyCreateRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.entity.ReviewReplyEntity
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewReplyRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
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
class ReviewReplyServiceImplTest {

    @Mock lateinit var reviewRepository: ReviewRepository
    @Mock lateinit var reviewReplyRepository: ReviewReplyRepository
    @Mock lateinit var reviewImageRepository: ReviewImageRepository
    @Mock lateinit var merchantProfileRepository: MerchantProfileRepository

    @InjectMocks lateinit var reviewReplyService: ReviewReplyServiceImpl

    private lateinit var consumer: UserEntity
    private lateinit var merchantUser: UserEntity
    private lateinit var otherMerchantUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var otherMerchant: MerchantProfileEntity
    private lateinit var product: ProductEntity
    private lateinit var review: ReviewEntity

    @BeforeEach
    fun setUp() {
        consumer = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        otherMerchantUser = UserEntity(id = 3L, email = "other@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        otherMerchant = MerchantProfileEntity(
            userId = 3L, user = otherMerchantUser, businessName = "Other Shop",
            businessRegNo = "987-65-43210",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        product = ProductEntity(
            id = 10L, merchant = merchant, title = "상품",
            price = 5000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        review = ReviewEntity(id = 100L, user = consumer, product = product, rating = 5, content = "좋아요", reservationId = 1L)
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    fun `create - 상품 소유 소상공인이 답글 작성 시 성공`() {
        val request = ReviewReplyCreateRequest(content = "감사합니다!")
        val reply = ReviewReplyEntity(id = 1L, review = review, merchant = merchant, content = "감사합니다!")

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewReplyRepository.existsByReviewId(100L)).thenReturn(false)
        whenever(merchantProfileRepository.findById(2L)).thenReturn(Optional.of(merchant))
        whenever(reviewReplyRepository.save(any())).thenReturn(reply)
        whenever(reviewImageRepository.findByReviewIdOrderByDisplayOrder(100L)).thenReturn(emptyList())

        val response = reviewReplyService.create(2L, 100L, request)

        assertEquals(100L, response.id)
        assertEquals("감사합니다!", response.reply?.content)
    }

    @Test
    fun `create - 다른 소상공인이 답글 작성 시 FORBIDDEN`() {
        val request = ReviewReplyCreateRequest(content = "테스트")

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))

        val ex = assertThrows<BusinessException> { reviewReplyService.create(3L, 100L, request) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `create - 중복 답글 작성 시 REVIEW_REPLY_ALREADY_EXISTS`() {
        val request = ReviewReplyCreateRequest(content = "두 번째 답글")

        whenever(reviewRepository.findById(100L)).thenReturn(Optional.of(review))
        whenever(reviewReplyRepository.existsByReviewId(100L)).thenReturn(true)

        val ex = assertThrows<BusinessException> { reviewReplyService.create(2L, 100L, request) }
        assertEquals(ErrorCode.REVIEW_REPLY_ALREADY_EXISTS, ex.errorCode)
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    fun `delete - 본인 답글 삭제 시 성공`() {
        val reply = ReviewReplyEntity(id = 1L, review = review, merchant = merchant, content = "감사합니다!")

        whenever(reviewReplyRepository.findByReviewId(100L)).thenReturn(Optional.of(reply))

        reviewReplyService.delete(2L, 100L)

        verify(reviewReplyRepository).delete(reply)
    }

    @Test
    fun `delete - 답글 미존재 시 REVIEW_REPLY_NOT_FOUND`() {
        whenever(reviewReplyRepository.findByReviewId(100L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { reviewReplyService.delete(2L, 100L) }
        assertEquals(ErrorCode.REVIEW_REPLY_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `delete - 타인 답글 삭제 시 FORBIDDEN`() {
        val reply = ReviewReplyEntity(id = 1L, review = review, merchant = merchant, content = "감사합니다!")

        whenever(reviewReplyRepository.findByReviewId(100L)).thenReturn(Optional.of(reply))

        val ex = assertThrows<BusinessException> { reviewReplyService.delete(3L, 100L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
