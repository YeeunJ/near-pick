package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.merchant.MerchantService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/merchant")
@PreAuthorize("hasRole('MERCHANT')")
class MerchantController(private val merchantService: MerchantService) {

    @GetMapping("/dashboard")
    fun getDashboard(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(merchantService.getDashboard(userId))

    @GetMapping("/profile")
    fun getProfile(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(merchantService.getProfile(userId))
}
