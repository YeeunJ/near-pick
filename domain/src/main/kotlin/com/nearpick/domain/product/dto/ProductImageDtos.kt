package com.nearpick.domain.product.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PresignedUrlRequest(
    @field:NotBlank @field:Size(max = 255)
    val filename: String,
    @field:NotBlank
    val contentType: String,
)

data class PresignedUrlResponse(
    val presignedUrl: String,
    val s3Key: String,
    val expiresInSeconds: Int,
)

data class ProductImageSaveRequest(
    @field:NotBlank val s3Key: String,
    val displayOrder: Int = 0,
)

data class ProductImageResponse(
    val id: Long,
    val url: String,
    val s3Key: String,
    val displayOrder: Int,
)

data class ImageOrderItem(
    val imageId: Long,
    val displayOrder: Int,
)
