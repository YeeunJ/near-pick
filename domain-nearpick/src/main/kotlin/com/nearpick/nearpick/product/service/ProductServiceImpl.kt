package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductListItem
import com.nearpick.domain.product.dto.ProductNearbyRequest
import com.nearpick.domain.product.dto.ProductStatusResponse
import com.nearpick.domain.product.dto.ProductSummaryResponse
import com.nearpick.nearpick.product.mapper.ProductMapper.toDetailResponse
import com.nearpick.nearpick.product.mapper.ProductMapper.toListItem
import com.nearpick.nearpick.product.mapper.ProductMapper.toStatusResponse
import com.nearpick.nearpick.product.mapper.ProductMapper.toSummaryResponse
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.transaction.repository.WishlistRepository
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository

@Service
@Transactional(readOnly = true)
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
    private val wishlistRepository: WishlistRepository,
    private val reservationRepository: ReservationRepository,
    private val flashPurchaseRepository: FlashPurchaseRepository,
) : ProductService {

    @Cacheable(
        value = ["products-nearby"],
        key = "#request.lat.toString().substring(0, T(Math).min(6, #request.lat.toString().length())) + ':' + #request.lng.toString().substring(0, T(Math).min(6, #request.lng.toString().length())) + ':' + #request.radius + ':' + #request.sort + ':' + #request.page + ':' + #request.size"
    )
    override fun getNearby(request: ProductNearbyRequest): Page<ProductSummaryResponse> {
        val pageable = PageRequest.of(request.page, request.size)
        return productRepository.findNearby(
            lat = request.lat.toDouble(),
            lng = request.lng.toDouble(),
            radius = request.radius,
            sort = request.sort.name.lowercase(),
            pageable = pageable,
        ).map { it.toSummaryResponse() }
    }

    @Cacheable(value = ["products-detail"], key = "#productId")
    override fun getDetail(productId: Long): ProductDetailResponse {
        val product = productRepository.findById(productId).orElseThrow {
            BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        return product.toDetailResponse(
            wishlistCount = wishlistRepository.countByProduct_Id(productId),
            reservationCount = reservationRepository.countByProduct_Id(productId),
            purchaseCount = flashPurchaseRepository.countByProduct_Id(productId),
        )
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["products-nearby"], allEntries = true),
    ])
    override fun create(merchantId: Long, request: ProductCreateRequest): ProductStatusResponse {
        val merchant = merchantProfileRepository.findById(merchantId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
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
        )
        product.status = ProductStatus.ACTIVE
        return productRepository.save(product).toStatusResponse()
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["products-detail"], key = "#productId"),
        CacheEvict(value = ["products-nearby"], allEntries = true),
    ])
    override fun close(merchantId: Long, productId: Long): ProductStatusResponse {
        val product = productRepository.findById(productId).orElseThrow {
            BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        if (product.merchant.userId != merchantId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        product.status = ProductStatus.CLOSED
        return product.toStatusResponse()
    }

    override fun getMyProducts(merchantId: Long, page: Int, size: Int): Page<ProductListItem> {
        val pageable = PageRequest.of(page, size)
        val products = productRepository.findAllByMerchant_UserId(merchantId, pageable)

        if (products.isEmpty) return products.map { it.toListItem(0L) }

        // Batch wishlist count — single query instead of N per product
        val productIds = products.content.map { it.id }
        val wishlistCounts = wishlistRepository.countByProductIds(productIds)
            .associate { it.productId to it.cnt }

        return products.map { product ->
            product.toListItem(wishlistCounts[product.id] ?: 0L)
        }
    }
}
