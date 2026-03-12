package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ImageStorageService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.ImageOrderItem
import com.nearpick.domain.product.dto.PresignedUrlRequest
import com.nearpick.domain.product.dto.ProductImageSaveRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.entity.ProductImageEntity
import com.nearpick.nearpick.product.repository.ProductImageRepository
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ProductImageServiceImplTest {

    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var productImageRepository: ProductImageRepository
    @Mock lateinit var imageStorageService: ImageStorageService

    private lateinit var service: ProductImageServiceImpl

    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        service = ProductImageServiceImpl(productRepository, productImageRepository, imageStorageService)

        val merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "테스트샵",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
        )
        product = ProductEntity(
            id = 1L, merchant = merchant, title = "테스트 상품",
            price = 5000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
        )
    }

    // ── generatePresignedUrl ──────────────────────────────────────────

    @Test
    fun `generatePresignedUrl - jpg 파일로 성공적으로 Presigned URL을 발급한다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.countByProductId(1L)).thenReturn(2L)
        whenever(imageStorageService.generatePresignedPutUrl(any(), any())).thenReturn("https://presigned.url/photo")

        val result = service.generatePresignedUrl(
            merchantId = 2L, productId = 1L,
            request = PresignedUrlRequest(filename = "photo.jpg", contentType = "image/jpeg"),
        )

        assertEquals("https://presigned.url/photo", result.presignedUrl)
        assertEquals(300, result.expiresInSeconds)
        assertTrue(result.s3Key.startsWith("products/1/images/"))
        assertTrue(result.s3Key.endsWith(".jpg"))
    }

    @Test
    fun `generatePresignedUrl - 존재하지 않는 상품이면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            service.generatePresignedUrl(2L, 99L, PresignedUrlRequest("a.jpg", "image/jpeg"))
        }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `generatePresignedUrl - 소유자가 아니면 FORBIDDEN 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> {
            service.generatePresignedUrl(
                merchantId = 99L, productId = 1L,
                request = PresignedUrlRequest("a.jpg", "image/jpeg"),
            )
        }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `generatePresignedUrl - 이미지 5장 초과 시 PRODUCT_IMAGE_LIMIT_EXCEEDED 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.countByProductId(1L)).thenReturn(5L)

        val ex = assertThrows<BusinessException> {
            service.generatePresignedUrl(2L, 1L, PresignedUrlRequest("a.jpg", "image/jpeg"))
        }
        assertEquals(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED, ex.errorCode)
    }

    @Test
    fun `generatePresignedUrl - 허용되지 않는 확장자면 INVALID_IMAGE_TYPE 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.countByProductId(1L)).thenReturn(0L)

        val ex = assertThrows<BusinessException> {
            service.generatePresignedUrl(2L, 1L, PresignedUrlRequest("photo.gif", "image/gif"))
        }
        assertEquals(ErrorCode.INVALID_IMAGE_TYPE, ex.errorCode)
    }

    // ── saveImageUrl ──────────────────────────────────────────────────

    @Test
    fun `saveImageUrl - 올바른 s3Key로 이미지 URL을 저장한다`() {
        val s3Key = "products/1/images/uuid-abc.jpg"
        val url = "https://bucket.s3.ap-northeast-2.amazonaws.com/$s3Key"
        val savedEntity = ProductImageEntity(id = 10L, product = product, s3Key = s3Key, url = url, displayOrder = 0)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.countByProductId(1L)).thenReturn(0L)
        whenever(imageStorageService.buildPublicUrl(s3Key)).thenReturn(url)
        whenever(productImageRepository.save(any())).thenReturn(savedEntity)

        val result = service.saveImageUrl(2L, 1L, ProductImageSaveRequest(s3Key = s3Key, displayOrder = 0))

        assertEquals(10L, result.id)
        assertEquals(url, result.url)
        assertEquals(s3Key, result.s3Key)
        assertEquals(0, result.displayOrder)
    }

    @Test
    fun `saveImageUrl - 다른 상품의 s3Key 경로이면 INVALID_INPUT 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.countByProductId(1L)).thenReturn(0L)

        val ex = assertThrows<BusinessException> {
            service.saveImageUrl(2L, 1L, ProductImageSaveRequest(s3Key = "products/99/images/abc.jpg"))
        }
        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    // ── deleteImage ───────────────────────────────────────────────────

    @Test
    fun `deleteImage - 성공 시 스토리지와 DB에서 이미지를 삭제한다`() {
        val s3Key = "products/1/images/abc.jpg"
        val image = ProductImageEntity(id = 10L, product = product, s3Key = s3Key, url = "https://...", displayOrder = 0)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.findByIdAndProductId(10L, 1L)).thenReturn(image)

        service.deleteImage(merchantId = 2L, productId = 1L, imageId = 10L)

        verify(imageStorageService).deleteObject(s3Key)
        verify(productImageRepository).delete(image)
    }

    @Test
    fun `deleteImage - 존재하지 않는 이미지이면 PRODUCT_IMAGE_NOT_FOUND 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.findByIdAndProductId(99L, 1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            service.deleteImage(merchantId = 2L, productId = 1L, imageId = 99L)
        }
        assertEquals(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, ex.errorCode)
    }

    // ── reorderImages ─────────────────────────────────────────────────

    @Test
    fun `reorderImages - 이미지 순서를 변경하고 결과를 반환한다`() {
        val img1 = ProductImageEntity(id = 1L, product = product, s3Key = "k1", url = "u1", displayOrder = 0)
        val img2 = ProductImageEntity(id = 2L, product = product, s3Key = "k2", url = "u2", displayOrder = 1)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(productImageRepository.findAllByProductIdOrderByDisplayOrder(1L))
            .thenReturn(listOf(img1, img2))

        val orders = listOf(
            ImageOrderItem(imageId = 1L, displayOrder = 1),
            ImageOrderItem(imageId = 2L, displayOrder = 0),
        )

        val result = service.reorderImages(merchantId = 2L, productId = 1L, orders = orders)

        assertEquals(2, result.size)
        // 순서 변경이 엔티티에 반영됨
        assertEquals(1, result.find { it.id == 1L }?.displayOrder)
        assertEquals(0, result.find { it.id == 2L }?.displayOrder)
    }

    // ── getImages ─────────────────────────────────────────────────────

    @Test
    fun `getImages - 상품의 이미지 목록을 순서대로 반환한다`() {
        val img1 = ProductImageEntity(id = 1L, product = product, s3Key = "k1", url = "u1", displayOrder = 0)
        val img2 = ProductImageEntity(id = 2L, product = product, s3Key = "k2", url = "u2", displayOrder = 1)

        whenever(productImageRepository.findAllByProductIdOrderByDisplayOrder(1L))
            .thenReturn(listOf(img1, img2))

        val result = service.getImages(1L)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }
}
