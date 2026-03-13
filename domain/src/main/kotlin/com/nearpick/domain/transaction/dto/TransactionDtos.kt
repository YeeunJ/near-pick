package com.nearpick.domain.transaction.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.ReservationStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ── Wishlist ──────────────────────────────────────────────────────────────────

data class WishlistAddRequest(
    @field:Positive val productId: Long,
)

data class WishlistAddResponse(val wishlistId: Long)

data class WishlistItem(
    val wishlistId: Long,
    val productId: Long,
    val productTitle: String,
    val productPrice: Int,
    val productType: ProductType,
    val productStatus: ProductStatus,
    val shopAddress: String?,
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
    val memo: String?,
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

data class FlashPurchaseStatusResponse(val purchaseId: Long?, val status: FlashPurchaseStatus)

// ── Phase 12: Purchase Lifecycle ──────────────────────────────────────────────

data class ReservationVisitRequest(
    @field:NotBlank @field:Size(min = 6, max = 6)
    val code: String,
)

data class FlashPurchasePickupRequest(
    @field:NotBlank @field:Size(min = 6, max = 6)
    val code: String,
)

data class ReservationDetailResponse(
    val reservationId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: ReservationStatus,
    val memo: String?,
    val visitScheduledAt: LocalDateTime?,
    val reservedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val visitCode: String?,
)

data class FlashPurchaseDetailResponse(
    val purchaseId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: FlashPurchaseStatus,
    val purchasedAt: LocalDateTime,
    val pickedUpAt: LocalDateTime?,
    val pickupCode: String?,
)
