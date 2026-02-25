package com.nearpick.nearpick.transaction

import com.nearpick.domain.transaction.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
    fun findAllByUser_Id(userId: Long): List<ReservationEntity>

    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reservedAt DESC
    """)
    fun findByMerchant(
        @Param("merchantId") merchantId: Long,
        @Param("status") status: ReservationStatus?,
    ): List<ReservationEntity>
}
