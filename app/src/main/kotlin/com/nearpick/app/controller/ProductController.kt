package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductNearbyRequest
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

@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {

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

    @GetMapping("/{productId}")
    fun getDetail(@PathVariable productId: Long) =
        ApiResponse.success(productService.getDetail(productId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MERCHANT')")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ProductCreateRequest,
    ) = ApiResponse.success(productService.create(userId, request))

    @PatchMapping("/{productId}/close")
    @PreAuthorize("hasRole('MERCHANT')")
    fun close(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) = ApiResponse.success(productService.close(userId, productId))

    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getMyProducts(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(productService.getMyProducts(userId, page, size))
}
