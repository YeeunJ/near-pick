package com.nearpick.domain.wishlist.dto

import jakarta.validation.constraints.Positive

data class WishlistAddRequest(
    @field:Positive val productId: Long,
)
