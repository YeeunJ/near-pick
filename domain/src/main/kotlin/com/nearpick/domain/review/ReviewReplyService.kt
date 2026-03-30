package com.nearpick.domain.review

import com.nearpick.domain.review.dto.ReviewReplyCreateRequest
import com.nearpick.domain.review.dto.ReviewResponse

interface ReviewReplyService {
    fun create(merchantId: Long, reviewId: Long, request: ReviewReplyCreateRequest): ReviewResponse
    fun delete(merchantId: Long, reviewId: Long)
}
