package com.nearpick.domain.product.dto

import com.nearpick.domain.product.ProductType
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

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
