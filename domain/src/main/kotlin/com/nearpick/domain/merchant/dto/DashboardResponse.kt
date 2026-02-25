package com.nearpick.domain.merchant.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import java.time.LocalDateTime

data class DashboardResponse(
    val businessName: String,
    val todayReservationCount: Long,
    val todayPurchaseCount: Long,
    val popularityScore: Double,
    val pendingReservations: List<PendingReservationItem>,
    val myProducts: List<DashboardProductItem>,
)

data class PendingReservationItem(
    val id: Long,
    // 이메일 원문 대신 마스킹 처리하여 PII 노출 방지 (예: us**@example.com)
    val consumerMaskedEmail: String,
    val productTitle: String,
    val visitAt: LocalDateTime?,
)

data class DashboardProductItem(
    val id: Long,
    val title: String,
    val status: ProductStatus,
    val productType: ProductType,
)
