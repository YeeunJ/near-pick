package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.location.LocationSource
import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.SortType
import com.nearpick.domain.product.dto.ProductAddStockRequest
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductNearbyRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Tag(name = "Products", description = "상품 관련 API")
@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {

    @Operation(summary = "주변 인기 상품 조회", description = "위치 기준 반경 내 활성 상품을 인기도순/거리순으로 조회한다. locationSource: DIRECT(기본, lat/lng 필수), CURRENT(현재 위치), SAVED(저장 위치, savedLocationId 필수)")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/nearby")
    fun getNearby(
        @AuthenticationPrincipal userId: Long?,
        @RequestParam(required = false) lat: BigDecimal?,
        @RequestParam(required = false) lng: BigDecimal?,
        @RequestParam(defaultValue = "5.0") radius: Double,
        @RequestParam(defaultValue = "POPULARITY") sort: SortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DIRECT") locationSource: LocationSource,
        @RequestParam(required = false) savedLocationId: Long?,
    ) = ApiResponse.success(
        productService.getNearby(
            request = ProductNearbyRequest(
                lat = lat, lng = lng, radius = radius, sort = sort, page = page, size = size,
                locationSource = locationSource, savedLocationId = savedLocationId,
            ),
            userId = userId,
        )
    )

    @Operation(summary = "상품 상세 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @SwaggerApiResponse(responseCode = "404", description = "상품 없음")
    @GetMapping("/{productId}")
    fun getDetail(@PathVariable productId: Long) =
        ApiResponse.success(productService.getDetail(productId))

    @Operation(summary = "상품 등록 (소상공인 전용)")
    @SecurityRequirement(name = "Bearer Authentication")
    @SwaggerApiResponse(responseCode = "201", description = "등록 성공")
    @SwaggerApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MERCHANT')")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ProductCreateRequest,
    ) = ApiResponse.success(productService.create(userId, request))

    @Operation(summary = "상품 마감 처리 (소상공인 전용)")
    @SecurityRequirement(name = "Bearer Authentication")
    @SwaggerApiResponse(responseCode = "200", description = "마감 처리 성공")
    @PatchMapping("/{productId}/close")
    @PreAuthorize("hasRole('MERCHANT')")
    fun close(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) = ApiResponse.success(productService.close(userId, productId))

    @Operation(summary = "내 상품 목록 조회 (소상공인 전용)")
    @SecurityRequirement(name = "Bearer Authentication")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getMyProducts(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(productService.getMyProducts(userId, page, size))

    @Operation(summary = "상품 일시정지 (소상공인 전용)", description = "ACTIVE → PAUSED")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{productId}/pause")
    @PreAuthorize("hasRole('MERCHANT')")
    fun pause(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) = ApiResponse.success(productService.pauseProduct(userId, productId))

    @Operation(summary = "상품 재개 (소상공인 전용)", description = "PAUSED → ACTIVE")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{productId}/resume")
    @PreAuthorize("hasRole('MERCHANT')")
    fun resume(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) = ApiResponse.success(productService.resumeProduct(userId, productId))

    @Operation(summary = "재고 추가 (소상공인 전용)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasRole('MERCHANT')")
    fun addStock(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @RequestBody @Valid request: ProductAddStockRequest,
    ) = ApiResponse.success(productService.addStock(userId, productId, request.additionalStock))
}
