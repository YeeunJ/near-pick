package com.nearpick.domain.auth.dto

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
