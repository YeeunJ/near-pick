package com.nearpick.nearpick.product

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductListItem
import com.nearpick.domain.product.dto.ProductNearbyRequest
import com.nearpick.domain.product.dto.ProductStatusResponse
import com.nearpick.domain.product.dto.ProductSummaryResponse
import com.nearpick.nearpick.transaction.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.ReservationRepository
import com.nearpick.nearpick.transaction.WishlistRepository
import com.nearpick.nearpick.user.MerchantProfileRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
    private val wishlistRepository: WishlistRepository,
    private val reservationRepository: ReservationRepository,
    private val flashPurchaseRepository: FlashPurchaseRepository,
) : ProductService {

    @Transactional(readOnly = true)
    override fun getNearby(request: ProductNearbyRequest): Page<ProductSummaryResponse> {
        val pageable = PageRequest.of(request.page, request.size)
        return productRepository
            .findNearby(request.lat.toDouble(), request.lng.toDouble(), request.radius, request.sort, pageable)
            .map { p ->
                ProductSummaryResponse(
                    id = p.id,
                    title = p.title,
                    price = p.price,
                    productType = ProductType.valueOf(p.productType),
                    status = ProductStatus.valueOf(p.status),
                    popularityScore = p.popularityScore,
                    distanceKm = p.distanceKm,
                    merchantName = p.merchantName,
                )
            }
    }

    @Transactional(readOnly = true)
    override fun getDetail(productId: Long): ProductDetailResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        val merchant = product.merchant
        return ProductDetailResponse(
            id = product.id,
            title = product.title,
            description = product.description,
            price = product.price,
            productType = product.productType,
            status = product.status,
            stock = product.stock,
            availableFrom = product.availableFrom,
            availableUntil = product.availableUntil,
            shopLat = merchant.shopLat,
            shopLng = merchant.shopLng,
            shopAddress = merchant.shopAddress,
            merchantName = merchant.businessName,
            wishlistCount = wishlistRepository.countByProduct_Id(productId),
            reservationCount = reservationRepository.countByProduct_Id(productId),
            purchaseCount = flashPurchaseRepository.countByProduct_Id(productId),
        )
    }

    @Transactional
    override fun create(merchantId: Long, request: ProductCreateRequest): ProductStatusResponse {
        val merchant = merchantProfileRepository.findById(merchantId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = ProductEntity(
            merchant = merchant,
            title = request.title,
            description = request.description,
            price = request.price,
            productType = request.productType,
            stock = request.stock,
            availableFrom = request.availableFrom,
            availableUntil = request.availableUntil,
            shopLat = merchant.shopLat,
            shopLng = merchant.shopLng,
            status = ProductStatus.ACTIVE,
        )
        val saved = productRepository.save(product)
        return ProductStatusResponse(id = saved.id, status = saved.status)
    }

    @Transactional
    override fun close(merchantId: Long, productId: Long): ProductStatusResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.merchant.userId != merchantId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        product.status = ProductStatus.CLOSED
        return ProductStatusResponse(id = product.id, status = product.status)
    }

    @Transactional(readOnly = true)
    override fun getMyProducts(merchantId: Long, page: Int, size: Int): Page<ProductListItem> {
        val pageable = PageRequest.of(page, size)
        return productRepository.findAllByMerchant_UserId(merchantId, pageable)
            .map { product ->
                ProductListItem(
                    id = product.id,
                    title = product.title,
                    price = product.price,
                    status = product.status,
                    productType = product.productType,
                    stock = product.stock,
                    wishlistCount = wishlistRepository.countByProduct_Id(product.id),
                )
            }
    }
}
