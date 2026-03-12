package com.nearpick.domain.product

import com.nearpick.domain.product.dto.ImageOrderItem
import com.nearpick.domain.product.dto.PresignedUrlRequest
import com.nearpick.domain.product.dto.PresignedUrlResponse
import com.nearpick.domain.product.dto.ProductImageResponse
import com.nearpick.domain.product.dto.ProductImageSaveRequest

interface ProductImageService {
    fun generatePresignedUrl(merchantId: Long, productId: Long, request: PresignedUrlRequest): PresignedUrlResponse
    fun saveImageUrl(merchantId: Long, productId: Long, request: ProductImageSaveRequest): ProductImageResponse
    fun deleteImage(merchantId: Long, productId: Long, imageId: Long)
    fun reorderImages(merchantId: Long, productId: Long, orders: List<ImageOrderItem>): List<ProductImageResponse>
    fun getImages(productId: Long): List<ProductImageResponse>
}
