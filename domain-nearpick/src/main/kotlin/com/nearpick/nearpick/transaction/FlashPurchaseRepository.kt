package com.nearpick.nearpick.transaction

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface FlashPurchaseRepository : JpaRepository<FlashPurchaseEntity, Long> {

    fun findAllByUser_Id(userId: Long, pageable: Pageable): Page<FlashPurchaseEntity>

    fun countByProduct_Id(productId: Long): Long

    @Query("""
        SELECT COUNT(fp)
        FROM FlashPurchaseEntity fp
        WHERE fp.product.merchant.userId = :merchantId
          AND fp.purchasedAt >= :from
          AND fp.purchasedAt < :to
    """)
    fun countByMerchantIdAndPeriod(
        @Param("merchantId") merchantId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long
}
