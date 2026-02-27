package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.merchant.MerchantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Merchants", description = "소상공인 전용 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/merchants")
@PreAuthorize("hasRole('MERCHANT')")
class MerchantController(private val merchantService: MerchantService) {

    @Operation(summary = "소상공인 대시보드", description = "등록 상품, 예약/구매 집계, 인기도 점수를 반환한다.")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me/dashboard")
    fun getDashboard(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(merchantService.getDashboard(userId))

    @Operation(summary = "소상공인 프로필 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me/profile")
    fun getProfile(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(merchantService.getProfile(userId))
}
