package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.review.ReviewReplyService
import com.nearpick.domain.review.dto.ReviewReplyCreateRequest
import com.nearpick.domain.review.dto.ReviewResponse
import com.nearpick.nearpick.review.entity.ReviewReplyEntity
import com.nearpick.nearpick.review.mapper.ReviewMapper
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewReplyRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewReplyServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val reviewReplyRepository: ReviewReplyRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
) : ReviewReplyService {

    @Transactional
    override fun create(merchantId: Long, reviewId: Long, request: ReviewReplyCreateRequest): ReviewResponse {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        if (reviewReplyRepository.existsByReviewId(reviewId)) throw BusinessException(ErrorCode.REVIEW_REPLY_ALREADY_EXISTS)

        val merchant = merchantProfileRepository.findById(merchantId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val reply = reviewReplyRepository.save(ReviewReplyEntity(review = review, merchant = merchant, content = request.content))

        val images = reviewImageRepository.findByReviewIdOrderByDisplayOrder(reviewId)
        return ReviewMapper.toResponse(review, images, reply)
    }

    @Transactional
    override fun delete(merchantId: Long, reviewId: Long) {
        val reply = reviewReplyRepository.findByReviewId(reviewId)
            .orElseThrow { BusinessException(ErrorCode.REVIEW_REPLY_NOT_FOUND) }
        if (reply.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        reviewReplyRepository.delete(reply)
    }
}
