package com.nearpick.domain.location.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class UpdateCurrentLocationRequest(
    @field:NotNull val lat: BigDecimal,
    @field:NotNull val lng: BigDecimal,
)

data class CreateSavedLocationRequest(
    @field:NotBlank @field:Size(max = 50) val label: String,
    @field:NotNull val lat: BigDecimal,
    @field:NotNull val lng: BigDecimal,
    val isDefault: Boolean = false,
)

data class UpdateSavedLocationRequest(
    @field:Size(max = 50) val label: String? = null,
    val isDefault: Boolean? = null,
)

data class SavedLocationResponse(
    val id: Long,
    val label: String,
    val lat: BigDecimal,
    val lng: BigDecimal,
    val isDefault: Boolean,
    val createdAt: LocalDateTime,
)

data class LocationSearchResult(
    val address: String,
    val lat: BigDecimal,
    val lng: BigDecimal,
)
