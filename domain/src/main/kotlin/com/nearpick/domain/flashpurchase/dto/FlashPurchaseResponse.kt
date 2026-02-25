package com.nearpick.domain.flashpurchase.dto

import com.nearpick.domain.transaction.FlashPurchaseStatus
import java.time.LocalDateTime

data class FlashPurchaseCreatedResponse(
    val id: Long,
    val status: FlashPurchaseStatus,
    val purchasedAt: LocalDateTime,
)

data class FlashPurchaseResponse(
    val id: Long,
    val productTitle: String,
    val merchantName: String,
    val status: FlashPurchaseStatus,
    val quantity: Int,
    val purchasedAt: LocalDateTime,
)
