package com.nearpick.domain.admin.dto

import com.nearpick.domain.product.ProductStatus

data class AdminProductResponse(
    val id: Long,
    val title: String,
    val merchantName: String,
    val status: ProductStatus,
    val price: Int,
)

data class AdminProductStatusResponse(
    val id: Long,
    val status: ProductStatus,
)
