package com.nearpick.nearpick.review.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ImageStorageService
import com.nearpick.domain.review.ReviewImageService
import com.nearpick.domain.review.dto.ReviewImageUploadResponse
import com.nearpick.nearpick.review.entity.ReviewImageEntity
import com.nearpick.nearpick.review.repository.ReviewImageRepository
import com.nearpick.nearpick.review.repository.ReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val MAX_REVIEW_IMAGES = 3

@Service
@Transactional(readOnly = true)
class ReviewImageServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val imageStorageService: ImageStorageService,
) : ReviewImageService {

    @Transactional
    override fun getPresignedUrl(userId: Long, reviewId: Long, contentType: String): ReviewImageUploadResponse {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        val count = reviewImageRepository.countByReviewId(reviewId)
        if (count >= MAX_REVIEW_IMAGES) throw BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED)

        val s3Key = "review-images/${reviewId}/${UUID.randomUUID()}"
        val presignedUrl = imageStorageService.generatePresignedPutUrl(s3Key, contentType)
        val imageUrl = imageStorageService.buildPublicUrl(s3Key)
        return ReviewImageUploadResponse(presignedUrl = presignedUrl, imageUrl = imageUrl, s3Key = s3Key)
    }

    @Transactional
    override fun confirmUpload(userId: Long, reviewId: Long, s3Key: String, imageUrl: String) {
        val review = reviewRepository.findById(reviewId).orElseThrow { BusinessException(ErrorCode.REVIEW_NOT_FOUND) }
        if (review.user.id != userId) throw BusinessException(ErrorCode.FORBIDDEN)
        val count = reviewImageRepository.countByReviewId(reviewId)
        if (count >= MAX_REVIEW_IMAGES) throw BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED)
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
