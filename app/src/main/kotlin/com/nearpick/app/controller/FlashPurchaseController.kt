package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchasePickupRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Flash Purchases", description = "선착순 구매 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/flash-purchases")
class FlashPurchaseController(private val flashPurchaseService: FlashPurchaseService) {

    @Operation(summary = "선착순 구매 (소비자 전용)", description = "Kafka 비동기 처리, 즉시 PENDING 반환")
    @SwaggerApiResponse(responseCode = "201", description = "구매 요청 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONSUMER')")
    fun purchase(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: FlashPurchaseCreateRequest,
    ) = ApiResponse.success(flashPurchaseService.purchase(userId, request.productId, request))

    @Operation(summary = "내 구매 내역 조회 (소비자 전용)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getMyPurchases(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(flashPurchaseService.getMyPurchases(userId, page, size))

    @Operation(summary = "구매 상세 조회 (소비자/소상공인)", description = "소비자 본인은 pickupCode 포함")
    @GetMapping("/{purchaseId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
    fun getDetail(
        @AuthenticationPrincipal userId: Long,
        @PathVariable purchaseId: Long,
    ) = ApiResponse.success(flashPurchaseService.getDetail(userId, purchaseId))

    @Operation(summary = "픽업 코드 확인 → PICKED_UP (소상공인 전용)")
    @PatchMapping("/pickup")
    @PreAuthorize("hasRole('MERCHANT')")
    fun pickup(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: FlashPurchasePickupRequest,
    ) = ApiResponse.success(flashPurchaseService.pickupByCode(userId, request))

    @Operation(summary = "소상공인 구매 취소 + 재고 복원 (CONFIRMED → CANCELLED)")
    @PatchMapping("/{purchaseId}/cancel-by-merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    fun cancelByMerchant(
        @AuthenticationPrincipal userId: Long,
        @PathVariable purchaseId: Long,
    ) = ApiResponse.success(flashPurchaseService.cancelByMerchant(userId, purchaseId))

    @Operation(summary = "소상공인 구매 목록 조회 (상태 필터)")
    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getMerchantPurchases(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(required = false) status: FlashPurchaseStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(flashPurchaseService.getMerchantPurchases(userId, status, page, size))
}
