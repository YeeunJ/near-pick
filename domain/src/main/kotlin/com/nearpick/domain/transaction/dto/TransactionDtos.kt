package com.nearpick.domain.transaction.dto

import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.ReservationStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

// ── Wishlist ──────────────────────────────────────────────────────────────────

data class WishlistAddRequest(
    @field:Positive val productId: Long,
)

data class WishlistItem(
    val wishlistId: Long,
    val productId: Long,
    val productTitle: String,
    val productPrice: Int,
    val productType: ProductType,
    val createdAt: LocalDateTime,
)

// ── Reservation ───────────────────────────────────────────────────────────────

data class ReservationCreateRequest(
    @field:Positive val productId: Long,
    @field:Positive @field:Max(99) val quantity: Int = 1,
    val memo: String? = null,
    val visitScheduledAt: LocalDateTime? = null,
)

data class ReservationItem(
    val reservationId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: ReservationStatus,
    val visitScheduledAt: LocalDateTime?,
    val reservedAt: LocalDateTime,
)

data class ReservationStatusResponse(val reservationId: Long, val status: ReservationStatus)

// ── FlashPurchase ─────────────────────────────────────────────────────────────

data class FlashPurchaseCreateRequest(
    @field:Positive val productId: Long,
    @field:Positive @field:Max(99) val quantity: Int = 1,
)

data class FlashPurchaseItem(
    val purchaseId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: FlashPurchaseStatus,
    val purchasedAt: LocalDateTime,
)

data class FlashPurchaseStatusResponse(val purchaseId: Long, val status: FlashPurchaseStatus)
