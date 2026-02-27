package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.WishlistService
import com.nearpick.domain.transaction.dto.WishlistAddRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Wishlists", description = "찜 관련 API (소비자 전용)")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/wishlists")
@PreAuthorize("hasRole('CONSUMER')")
class WishlistController(private val wishlistService: WishlistService) {

    @Operation(summary = "상품 찜 추가")
    @SwaggerApiResponse(responseCode = "201", description = "찜 추가 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: WishlistAddRequest,
    ) = ApiResponse.success(mapOf("wishlistId" to wishlistService.add(userId, request.productId)))

    @Operation(summary = "상품 찜 취소")
    @SwaggerApiResponse(responseCode = "200", description = "찜 취소 성공")
    @DeleteMapping("/{productId}")
    fun remove(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        wishlistService.remove(userId, productId)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "내 찜 목록 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    fun getMyWishlists(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(wishlistService.getMyWishlists(userId))
}
