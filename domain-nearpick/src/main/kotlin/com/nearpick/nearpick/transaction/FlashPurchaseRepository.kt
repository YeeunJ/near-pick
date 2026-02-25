package com.nearpick.nearpick.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface FlashPurchaseRepository : JpaRepository<FlashPurchaseEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
    fun findAllByUser_Id(userId: Long): List<FlashPurchaseEntity>
}
