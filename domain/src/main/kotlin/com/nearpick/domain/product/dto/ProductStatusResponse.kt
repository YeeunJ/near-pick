package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductStatus

data class ProductStatusResponse(
    val id: Long,
    val status: ProductStatus,
)
