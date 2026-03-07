package com.nearpick.nearpick.transaction.messaging

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class FlashPurchaseRequestEvent @JsonCreator constructor(
    @JsonProperty("idempotencyKey") val idempotencyKey: String,   // "{userId}-{productId}-{yyyyMMdd}"
    @JsonProperty("userId") val userId: Long,
    @JsonProperty("productId") val productId: Long,
    @JsonProperty("quantity") val quantity: Int,
    @JsonProperty("requestedAt") val requestedAt: LocalDateTime = LocalDateTime.now(),
)
