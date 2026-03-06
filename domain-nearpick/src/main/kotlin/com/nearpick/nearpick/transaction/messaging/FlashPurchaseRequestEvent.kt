package com.nearpick.nearpick.transaction.messaging

import java.time.LocalDateTime

data class FlashPurchaseRequestEvent(
    val idempotencyKey: String,   // "{userId}-{productId}-{yyyyMMdd}"
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
)
