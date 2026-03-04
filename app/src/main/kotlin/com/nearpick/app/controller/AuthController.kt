package com.nearpick.app.controller

import com.nearpick.app.config.JwtTokenProvider
import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.auth.AuthService
import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.auth.dto.SignupMerchantRequest
import com.nearpick.domain.auth.dto.TokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "인증 관련 API (회원가입 / 로그인)")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    @Operation(summary = "소비자 회원가입")
    @SwaggerApiResponse(responseCode = "201", description = "회원가입 성공")
    @SwaggerApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @SwaggerApiResponse(responseCode = "409", description = "이메일 중복")
    @PostMapping("/signup/consumer")
    @ResponseStatus(HttpStatus.CREATED)
    fun signupConsumer(@RequestBody @Valid request: SignupConsumerRequest) =
        ApiResponse.success(authService.signupConsumer(request))

    @Operation(summary = "소상공인 회원가입")
    @SwaggerApiResponse(responseCode = "201", description = "회원가입 성공")
    @SwaggerApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @SwaggerApiResponse(responseCode = "409", description = "이메일 또는 사업자등록번호 중복")
    @PostMapping("/signup/merchant")
    @ResponseStatus(HttpStatus.CREATED)
    fun signupMerchant(@RequestBody @Valid request: SignupMerchantRequest) =
        ApiResponse.success(authService.signupMerchant(request))

    @Operation(summary = "로그인", description = "이메일/비밀번호 로그인 후 JWT 토큰을 반환한다.")
    @SwaggerApiResponse(responseCode = "200", description = "로그인 성공, accessToken 반환")
    @SwaggerApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ApiResponse<TokenResponse> {
        val result = authService.login(request)
        val token = jwtTokenProvider.createToken(result.userId, result.role)
        return ApiResponse.success(TokenResponse(accessToken = token))
    }
}
