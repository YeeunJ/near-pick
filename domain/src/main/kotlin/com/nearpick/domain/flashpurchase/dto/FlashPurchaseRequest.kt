package com.nearpick.domain.flashpurchase.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class FlashPurchaseRequest(
    @field:NotNull val productId: Long,
    @field:Min(1) val quantity: Int = 1,
)
