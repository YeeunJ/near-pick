package com.nearpick.domain.merchant.dto

import com.nearpick.domain.product.dto.ProductListItem
import java.math.BigDecimal

data class MerchantDashboardResponse(
    val merchantId: Long,
    val businessName: String,
    val totalPopularityScore: Double,
    val thisMonthReservationCount: Long,
    val thisMonthPurchaseCount: Long,
    val products: List<ProductListItem>,
)

data class MerchantProfileResponse(
    val merchantId: Long,
    val email: String,
    val businessName: String,
    val businessRegNo: String,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
    val shopAddress: String?,
    val rating: BigDecimal,
    val isVerified: Boolean,
)
