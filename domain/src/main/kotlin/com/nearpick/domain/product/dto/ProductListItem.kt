package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType

data class ProductListItem(
    val id: Long,
    val title: String,
    val price: Int,
    val status: ProductStatus,
    val productType: ProductType,
    val stock: Int,
    val wishlistCount: Long,
)
