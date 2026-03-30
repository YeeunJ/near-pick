package com.nearpick.domain.review

import com.nearpick.domain.review.dto.AdminReviewItem
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
    fun getAdminQueue(page: Int, size: Int): Page<AdminReviewItem>
}
