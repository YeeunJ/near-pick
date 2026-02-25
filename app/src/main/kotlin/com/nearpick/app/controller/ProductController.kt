package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductNearbyRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
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
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService,
) {

    @GetMapping("/nearby")
    fun getNearby(
        @RequestParam lat: BigDecimal,
        @RequestParam lng: BigDecimal,
        @RequestParam(defaultValue = "5.0") @Positive @Max(50) radius: Double,
        @RequestParam(defaultValue = "popularity") sort: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ) = ApiResponse.success(
        productService.getNearby(
            ProductNearbyRequest(
                lat = lat,
                lng = lng,
                radius = radius,
                sort = sort,
                page = page,
                size = size,
            ),
        ),
    )

    @GetMapping("/{id}")
    fun getDetail(@PathVariable id: Long) =
        ApiResponse.success(productService.getDetail(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal merchantId: Long,
        @RequestBody @Valid request: ProductCreateRequest,
    ) = ApiResponse.success(productService.create(merchantId, request))

    @PatchMapping("/{id}/close")
    fun close(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable id: Long,
    ) = ApiResponse.success(productService.close(merchantId, id))

    @GetMapping("/me")
    fun getMyProducts(
        @AuthenticationPrincipal merchantId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ) = ApiResponse.success(productService.getMyProducts(merchantId, page, size))
}
