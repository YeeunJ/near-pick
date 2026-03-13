package com.nearpick.nearpick.transaction.repository

import com.nearpick.domain.transaction.FlashPurchaseStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity

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

    /** 픽업 코드로 구매 조회 */
    fun findByPickupCode(pickupCode: String): FlashPurchaseEntity?

    /** 소상공인 구매 목록 (상태 필터, null이면 전체) */
    @Query("""
        SELECT f FROM FlashPurchaseEntity f
        WHERE f.product.merchant.userId = :merchantId
        AND (:status IS NULL OR f.status = :status)
        ORDER BY f.purchasedAt DESC
    """)
    fun findByMerchantIdAndOptionalStatus(
        @Param("merchantId") merchantId: Long,
        @Param("status") status: FlashPurchaseStatus?,
        pageable: Pageable,
    ): Page<FlashPurchaseEntity>
}
