package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ImageStorageService
import com.nearpick.domain.product.ProductImageService
import com.nearpick.domain.product.dto.ImageOrderItem
import com.nearpick.domain.product.dto.PresignedUrlRequest
import com.nearpick.domain.product.dto.PresignedUrlResponse
import com.nearpick.domain.product.dto.ProductImageResponse
import com.nearpick.domain.product.dto.ProductImageSaveRequest
import com.nearpick.nearpick.product.entity.ProductImageEntity
import com.nearpick.nearpick.product.repository.ProductImageRepository
import com.nearpick.nearpick.product.repository.ProductRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
private const val MAX_IMAGES = 5
private const val PRESIGNED_EXPIRES_SECONDS = 300

@Service
@Transactional(readOnly = true)
class ProductImageServiceImpl(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val imageStorageService: ImageStorageService,
) : ProductImageService {

    @Transactional
    override fun generatePresignedUrl(
        merchantId: Long,
        productId: Long,
        request: PresignedUrlRequest,
    ): PresignedUrlResponse {
        val product = findProductAndVerifyOwner(productId, merchantId)

        val count = productImageRepository.countByProductId(product.id)
        if (count >= MAX_IMAGES) throw BusinessException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED)

        val ext = request.filename.substringAfterLast('.', "").lowercase()
        if (ext !in ALLOWED_EXTENSIONS) throw BusinessException(ErrorCode.INVALID_IMAGE_TYPE)

        val uuid = java.util.UUID.randomUUID()
        val s3Key = "products/$productId/images/$uuid.$ext"
        val presignedUrl = imageStorageService.generatePresignedPutUrl(s3Key, request.contentType)

        return PresignedUrlResponse(
            presignedUrl = presignedUrl,
            s3Key = s3Key,
            expiresInSeconds = PRESIGNED_EXPIRES_SECONDS,
        )
    }

    @Transactional
    @CacheEvict(cacheNames = ["products-detail"], key = "#productId")
    override fun saveImageUrl(
        merchantId: Long,
        productId: Long,
        request: ProductImageSaveRequest,
    ): ProductImageResponse {
        val product = findProductAndVerifyOwner(productId, merchantId)

        val count = productImageRepository.countByProductId(product.id)
        if (count >= MAX_IMAGES) throw BusinessException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED)

        // s3Key는 반드시 해당 상품의 경로여야 함
        val expectedPrefix = "products/$productId/images/"
        if (!request.s3Key.startsWith(expectedPrefix)) throw BusinessException(ErrorCode.INVALID_INPUT)

        val url = imageStorageService.buildPublicUrl(request.s3Key)
        val image = ProductImageEntity(
            product = product,
            s3Key = request.s3Key,
            url = url,
            displayOrder = request.displayOrder,
        )
        return productImageRepository.save(image).toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["products-detail"], key = "#productId")
    override fun deleteImage(merchantId: Long, productId: Long, imageId: Long) {
        findProductAndVerifyOwner(productId, merchantId)
        val image = productImageRepository.findByIdAndProductId(imageId, productId)
            ?: throw BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND)

        imageStorageService.deleteObject(image.s3Key)
        productImageRepository.delete(image)
    }

    @Transactional
    @CacheEvict(cacheNames = ["products-detail"], key = "#productId")
    override fun reorderImages(
        merchantId: Long,
        productId: Long,
        orders: List<ImageOrderItem>,
    ): List<ProductImageResponse> {
        findProductAndVerifyOwner(productId, merchantId)
        val imageMap = productImageRepository
            .findAllByProductIdOrderByDisplayOrder(productId)
            .associateBy { it.id }

        orders.forEach { item ->
            imageMap[item.imageId]?.displayOrder = item.displayOrder
        }
        return productImageRepository
            .findAllByProductIdOrderByDisplayOrder(productId)
            .map { it.toResponse() }
    }

    override fun getImages(productId: Long): List<ProductImageResponse> =
        productImageRepository
            .findAllByProductIdOrderByDisplayOrder(productId)
            .map { it.toResponse() }

    private fun findProductAndVerifyOwner(productId: Long, merchantId: Long) =
        productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
            .also { if (it.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN) }

    private fun ProductImageEntity.toResponse() = ProductImageResponse(
        id = id,
        url = url,
        s3Key = s3Key,
        displayOrder = displayOrder,
    )
}
