package com.nearpick.nearpick.transaction

import com.nearpick.domain.transaction.ReservationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {

    fun findAllByUser_Id(userId: Long, pageable: Pageable): Page<ReservationEntity>

    fun countByProduct_Id(productId: Long): Long

    /** JPQL with @Param to avoid hardcoded FQN — safe across package refactors */
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
          AND r.status = :status
        ORDER BY r.reservedAt DESC
    """)
    fun findByMerchantIdAndStatus(
        @Param("merchantId") merchantId: Long,
        @Param("status") status: ReservationStatus,
        pageable: Pageable,
    ): Page<ReservationEntity>

    @Query("""
        SELECT COUNT(r)
        FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
          AND r.reservedAt >= :from
          AND r.reservedAt < :to
    """)
    fun countByMerchantIdAndPeriod(
        @Param("merchantId") merchantId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long

    @Query("""
        SELECT COUNT(r)
        FROM ReservationEntity r
        WHERE r.product.id = :productId
          AND r.status = :status
    """)
    fun countByProductIdAndStatus(
        @Param("productId") productId: Long,
        @Param("status") status: ReservationStatus,
    ): Long
}
