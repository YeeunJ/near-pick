package com.nearpick.domain.auth.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class SignupMerchantRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val businessName: String,
    @field:NotBlank val businessRegNo: String,
    @field:NotBlank val shopAddress: String,
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0") val shopLat: BigDecimal,
    @field:DecimalMin("-180.0") @field:DecimalMax("180.0") val shopLng: BigDecimal,
)
