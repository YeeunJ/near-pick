package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Flash Purchases", description = "선착순 구매 API (소비자 전용)")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/flash-purchases")
@PreAuthorize("hasRole('CONSUMER')")
class FlashPurchaseController(private val flashPurchaseService: FlashPurchaseService) {

    @Operation(summary = "선착순 구매", description = "재고 차감 후 구매 처리. 동시성 제어(비관적 락) 적용.")
    @SwaggerApiResponse(responseCode = "201", description = "구매 성공")
    @SwaggerApiResponse(responseCode = "400", description = "재고 부족 또는 비활성 상품")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun purchase(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: FlashPurchaseCreateRequest,
    ) = ApiResponse.success(flashPurchaseService.purchase(userId, request.productId, request))

    @Operation(summary = "내 구매 내역 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    fun getMyPurchases(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(flashPurchaseService.getMyPurchases(userId, page, size))
}
