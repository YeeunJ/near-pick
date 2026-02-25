package com.nearpick.domain.wishlist.dto

import com.nearpick.domain.product.ProductStatus
import java.time.LocalDateTime

data class WishlistAddedResponse(
    val productId: Long,
    val addedAt: LocalDateTime,
)

data class WishlistItemResponse(
    val productId: Long,
    val title: String,
    val price: Int,
    val merchantName: String,
    val status: ProductStatus,
)
