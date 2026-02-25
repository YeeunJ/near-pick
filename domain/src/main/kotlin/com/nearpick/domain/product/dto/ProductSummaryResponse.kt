package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType

data class ProductSummaryResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val popularityScore: Double,
    val distanceKm: Double,
    val merchantName: String,
)
