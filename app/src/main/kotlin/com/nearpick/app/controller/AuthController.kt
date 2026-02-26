package com.nearpick.app.controller

import com.nearpick.app.config.JwtTokenProvider
import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.auth.AuthService
import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.auth.dto.SignupMerchantRequest
import com.nearpick.domain.auth.dto.TokenResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    @PostMapping("/signup/consumer")
    @ResponseStatus(HttpStatus.CREATED)
    fun signupConsumer(@RequestBody @Valid request: SignupConsumerRequest) =
        ApiResponse.success(authService.signupConsumer(request))

    @PostMapping("/signup/merchant")
    @ResponseStatus(HttpStatus.CREATED)
    fun signupMerchant(@RequestBody @Valid request: SignupMerchantRequest) =
        ApiResponse.success(authService.signupMerchant(request))

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ApiResponse<TokenResponse> {
        val result = authService.login(request)
        val token = jwtTokenProvider.createToken(result.userId, result.role)
        return ApiResponse.success(TokenResponse(accessToken = token))
    }
}
