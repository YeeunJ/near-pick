package com.nearpick.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupConsumerRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
)
