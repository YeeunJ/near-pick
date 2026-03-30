package com.nearpick.nearpick.review.repository

import com.nearpick.nearpick.review.entity.ReviewImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewImageRepository : JpaRepository<ReviewImageEntity, Long> {
    fun countByReviewId(reviewId: Long): Long
    fun findByReviewIdOrderByDisplayOrder(reviewId: Long): List<ReviewImageEntity>
}
