package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.review.ReviewAiService
import com.nearpick.domain.review.ReviewService
import com.nearpick.domain.review.ReviewStatus
import com.nearpick.domain.review.dto.AdminReviewItem
import com.nearpick.domain.review.dto.ReviewCreateRequest
import com.nearpick.domain.review.dto.ReviewListItem
import com.nearpick.domain.review.dto.ReviewResponse
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.mapper.ReviewMapper
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewReplyRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
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

    @Transactional
    override fun create(userId: Long, request: ReviewCreateRequest): ReviewResponse {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = productRepository.findById(request.productId).orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }

        val reservationId = request.reservationId
        val flashPurchaseId = request.flashPurchaseId
        when {
            reservationId != null -> {
                val reservation = reservationRepository.findById(reservationId)
                    .orElseThrow { BusinessException(ErrorCode.RESERVATION_NOT_FOUND) }
                if (reservation.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
                if (reservation.status != ReservationStatus.COMPLETED) throw BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE)
                if (reviewRepository.existsByReservationId(reservationId)) throw BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS)
            }
            flashPurchaseId != null -> {
                val fp = flashPurchaseRepository.findById(flashPurchaseId)
                    .orElseThrow { BusinessException(ErrorCode.FLASH_PURCHASE_NOT_FOUND) }
                if (fp.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
                if (fp.status != FlashPurchaseStatus.PICKED_UP) throw BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE)
                if (reviewRepository.existsByFlashPurchaseId(flashPurchaseId)) throw BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS)
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

        updateProductRating(product.id)
        reviewAiService.checkAsync(review.id)

        return ReviewMapper.toResponse(review, emptyList(), null)
    }

    override fun getProductReviews(productId: Long, page: Int, size: Int): Page<ReviewListItem> {
        val pageable = PageRequest.of(page, size)
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.ACTIVE, pageable).map { review ->
            val images = reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)
            val reply = reviewReplyRepository.findByReviewId(review.id).orElse(null)
            ReviewMapper.toListItem(review, images, reply)
        }
    }

    override fun getMyReviews(userId: Long, page: Int, size: Int): Page<ReviewListItem> {
        val pageable = PageRequest.of(page, size)
        return reviewRepository.findByUserIdAndStatusNot(userId, ReviewStatus.DELETED, pageable).map { review ->
            val images = reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)
            val reply = reviewReplyRepository.findByReviewId(review.id).orElse(null)
            ReviewMapper.toListItem(review, images, reply)
        }
    }

    @Transactional
    override fun delete(userId: Long, reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        review.status = ReviewStatus.DELETED
        updateProductRating(review.product.id)
    }

    @Transactional
    override fun report(userId: Long, reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.reportCount++
    }

    @Transactional
    override fun adminBlind(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.status = ReviewStatus.BLINDED
        review.blindedReason = "ADMIN_REVIEWED"
        review.blindPending = false
        updateProductRating(review.product.id)
    }

    @Transactional
    override fun adminUnblind(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        review.status = ReviewStatus.ACTIVE
        review.blindedReason = null
        review.blindPending = false
        updateProductRating(review.product.id)
    }

    override fun getAdminQueue(page: Int, size: Int): Page<AdminReviewItem> {
        val pageable = PageRequest.of(page, size)
        return reviewRepository.findAdminQueueReviews(pageable = pageable).map { review ->
            val images = reviewImageRepository.findByReviewIdOrderByDisplayOrder(review.id)
            ReviewMapper.toAdminItem(review, images)
        }
    }

    private fun updateProductRating(productId: Long) {
        val product = productRepository.findById(productId).orElse(null) ?: return
        val avg = reviewRepository.findAverageRatingByProductId(productId) ?: 0.0
        val count = reviewRepository.countActiveByProductId(productId)
        product.averageRating = BigDecimal(avg).setScale(2, RoundingMode.HALF_UP)
        product.reviewCount = count.toInt()
    }
}
