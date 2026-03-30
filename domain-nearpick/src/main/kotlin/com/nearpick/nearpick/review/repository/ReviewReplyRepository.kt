package com.nearpick.nearpick.review.repository

import com.nearpick.nearpick.review.entity.ReviewReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReviewReplyRepository : JpaRepository<ReviewReplyEntity, Long> {
    fun existsByReviewId(reviewId: Long): Boolean
    fun findByReviewId(reviewId: Long): Optional<ReviewReplyEntity>
}
