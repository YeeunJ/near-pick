package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.product.ProductService
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

    @Operation(summary = "주변 인기 상품 조회", description = "현재 위치(lat, lng) 기준 반경 내 활성 상품을 인기도순/거리순으로 조회한다.")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/nearby")
    fun getNearby(
        @RequestParam lat: BigDecimal,
        @RequestParam lng: BigDecimal,
        @RequestParam(defaultValue = "5.0") radius: Double,
        @RequestParam(defaultValue = "popularity") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(
        productService.getNearby(
            ProductNearbyRequest(lat = lat, lng = lng, radius = radius, sort = sort, page = page, size = size)
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
}
