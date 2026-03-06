package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.SortType
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductNearbyRequest(
    val lat: BigDecimal,
    val lng: BigDecimal,
    @field:Positive @field:Max(50) val radius: Double = 5.0,
    val sort: SortType = SortType.POPULARITY,
    @field:Min(0) val page: Int = 0,
    @field:Positive @field:Max(100) val size: Int = 20,
)

data class ProductCreateRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    @field:Min(0) val price: Int,
    @field:NotNull val productType: ProductType,
    @field:Min(0) val stock: Int = 0,
    val availableFrom: LocalDateTime? = null,
    val availableUntil: LocalDateTime? = null,
) {
    @AssertTrue(message = "availableUntil must be equal to or after availableFrom")
    fun isAvailabilityRangeValid(): Boolean {
        if (availableFrom == null || availableUntil == null) return true
        return !availableUntil.isBefore(availableFrom)
    }
}

data class ProductSummaryResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val popularityScore: Double,
    val distanceKm: Double,
    val merchantName: String,
    val shopAddress: String?,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
)

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

data class ProductListItem(
    val id: Long,
    val title: String,
    val price: Int,
    val status: ProductStatus,
    val productType: ProductType,
    val stock: Int,
    val wishlistCount: Long,
)

data class ProductStatusResponse(val id: Long, val status: ProductStatus)
