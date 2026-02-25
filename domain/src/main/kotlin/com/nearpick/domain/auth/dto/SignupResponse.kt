package com.nearpick.domain.auth.dto

import com.nearpick.domain.user.UserRole

data class SignupResponse(
    val userId: Long,
    val email: String,
    val role: UserRole,
)
