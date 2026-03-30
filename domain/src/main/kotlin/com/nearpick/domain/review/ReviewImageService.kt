package com.nearpick.domain.review

import com.nearpick.domain.review.dto.ReviewImageUploadResponse

interface ReviewImageService {
    fun getPresignedUrl(userId: Long, reviewId: Long, contentType: String): ReviewImageUploadResponse
    fun confirmUpload(userId: Long, reviewId: Long, s3Key: String, imageUrl: String)
}
