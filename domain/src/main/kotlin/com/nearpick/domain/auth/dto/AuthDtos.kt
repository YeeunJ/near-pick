package com.nearpick.domain.auth.dto

import com.nearpick.domain.user.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class SignupConsumerRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
)

data class SignupMerchantRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val businessName: String,
    // 사업자등록번호 형식은 도메인 레이어(BusinessRegNo VO)에서 검증
    @field:NotBlank val businessRegNo: String,
    @field:NotBlank val shopAddress: String,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
)

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class SignupResponse(val userId: Long, val email: String, val role: UserRole)
data class TokenResponse(val accessToken: String)

/** 로그인 성공 후 서비스 계층이 반환하는 순수 결과 (JWT 생성은 Controller 책임) */
data class LoginResult(val userId: Long, val email: String, val role: UserRole)
