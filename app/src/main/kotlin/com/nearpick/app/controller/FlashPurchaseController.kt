package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/flash-purchases")
@PreAuthorize("hasRole('CONSUMER')")
class FlashPurchaseController(private val flashPurchaseService: FlashPurchaseService) {

    @PostMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun purchase(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: FlashPurchaseCreateRequest,
    ) = ApiResponse.success(flashPurchaseService.purchase(userId, productId, request))

    @GetMapping("/my")
    fun getMyPurchases(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(flashPurchaseService.getMyPurchases(userId, page, size))
}
