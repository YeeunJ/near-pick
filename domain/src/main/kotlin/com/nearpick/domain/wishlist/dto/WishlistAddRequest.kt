package com.nearpick.domain.wishlist.dto

import jakarta.validation.constraints.NotNull

data class WishlistAddRequest(
    @field:NotNull val productId: Long,
)
