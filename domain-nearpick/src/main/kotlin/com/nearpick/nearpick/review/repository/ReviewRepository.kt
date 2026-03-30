package com.nearpick.nearpick.review.repository

import com.nearpick.domain.review.ReviewStatus
import com.nearpick.nearpick.review.entity.ReviewEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<ReviewEntity, Long> {

    fun existsByReservationId(reservationId: Long): Boolean
    fun existsByFlashPurchaseId(flashPurchaseId: Long): Boolean

    fun findByProductIdAndStatus(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<ReviewEntity>

    fun findByUserIdAndStatusNot(
        userId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<ReviewEntity>

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    fun findAverageRatingByProductId(productId: Long): Double?

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    fun countActiveByProductId(productId: Long): Long

    @Query("""
        SELECT r FROM ReviewEntity r
        WHERE r.blindPending = true OR r.reportCount >= :threshold
        ORDER BY r.reportCount DESC, r.createdAt DESC
    """)
    fun findAdminQueueReviews(threshold: Int = 3, pageable: Pageable): Page<ReviewEntity>
}
