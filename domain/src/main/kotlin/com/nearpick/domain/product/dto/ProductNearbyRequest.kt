package com.nearpick.domain.product.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class ProductNearbyRequest(
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0") val lat: BigDecimal,
    @field:DecimalMin("-180.0") @field:DecimalMax("180.0") val lng: BigDecimal,
    @field:Positive @field:Max(50) val radius: Double = 5.0,
    @field:Pattern(regexp = "popularity|distance", message = "sort must be 'popularity' or 'distance'")
    val sort: String = "popularity",
    @field:Min(0) val page: Int = 0,
    @field:Positive @field:Max(100) val size: Int = 20,
)
