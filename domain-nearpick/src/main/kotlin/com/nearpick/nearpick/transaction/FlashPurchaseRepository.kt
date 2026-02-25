package com.nearpick.nearpick.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface FlashPurchaseRepository : JpaRepository<FlashPurchaseEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
    fun findAllByUser_Id(userId: Long): List<FlashPurchaseEntity>

    @Query("""
        SELECT COUNT(p) FROM FlashPurchaseEntity p
        WHERE p.product.merchant.userId = :merchantId
          AND p.purchasedAt >= :startOfDay
          AND p.purchasedAt < :endOfDay
    """)
    fun countTodayByMerchant(
        @Param("merchantId") merchantId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): Long
}
