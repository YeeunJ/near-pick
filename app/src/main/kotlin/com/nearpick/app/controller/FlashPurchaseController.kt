package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.flashpurchase.FlashPurchaseService
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/flash-purchases")
class FlashPurchaseController(
    private val flashPurchaseService: FlashPurchaseService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun purchase(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: FlashPurchaseRequest,
    ) = ApiResponse.success(flashPurchaseService.purchase(userId, request))

    @GetMapping("/me")
    fun getMyList(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(flashPurchaseService.getMyList(userId))
}
