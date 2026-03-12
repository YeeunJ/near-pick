package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.product.ProductImageService
import com.nearpick.domain.product.dto.ImageOrderItem
import com.nearpick.domain.product.dto.PresignedUrlRequest
import com.nearpick.domain.product.dto.PresignedUrlResponse
import com.nearpick.domain.product.dto.ProductImageResponse
import com.nearpick.domain.product.dto.ProductImageSaveRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Product Images", description = "상품 이미지 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/products/{productId}/images")
@PreAuthorize("hasRole('MERCHANT')")
class ProductImageController(
    private val productImageService: ProductImageService,
) {

    @Operation(summary = "이미지 업로드 URL 발급", description = "S3 Presigned URL (또는 local 업로드 URL)을 발급한다. 최대 5장.")
    @PostMapping("/presigned")
    fun generatePresignedUrl(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: PresignedUrlRequest,
    ): ResponseEntity<ApiResponse<PresignedUrlResponse>> =
        ResponseEntity.ok(ApiResponse.success(productImageService.generatePresignedUrl(userId, productId, request)))

    @Operation(summary = "이미지 URL 저장", description = "업로드 완료 후 서버에 URL을 저장한다.")
    @PostMapping
    fun saveImage(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: ProductImageSaveRequest,
    ): ResponseEntity<ApiResponse<ProductImageResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productImageService.saveImageUrl(userId, productId, request)))

    @Operation(summary = "이미지 삭제")
    @DeleteMapping("/{imageId}")
    fun deleteImage(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @PathVariable imageId: Long,
    ): ResponseEntity<Void> {
        productImageService.deleteImage(userId, productId, imageId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "이미지 순서 변경")
    @PutMapping("/order")
    fun reorderImages(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @RequestBody orders: List<ImageOrderItem>,
    ): ResponseEntity<ApiResponse<List<ProductImageResponse>>> =
        ResponseEntity.ok(ApiResponse.success(productImageService.reorderImages(userId, productId, orders)))
}
