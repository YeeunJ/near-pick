package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductDetailResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val stock: Int,
    val availableFrom: LocalDateTime?,
    val availableUntil: LocalDateTime?,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
    val shopAddress: String?,
    val merchantName: String,
    val wishlistCount: Long,
    val reservationCount: Long,
    val purchaseCount: Long,
)
