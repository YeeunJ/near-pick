package com.nearpick.nearpick.product.mapper

import com.nearpick.domain.admin.dto.AdminProductItem
import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductImageResponse
import com.nearpick.domain.product.dto.ProductListItem
import com.nearpick.domain.product.dto.ProductMenuOptionGroupResponse
import com.nearpick.domain.product.dto.ProductSpecItem
import com.nearpick.domain.product.dto.ProductStatusResponse
import com.nearpick.domain.product.dto.ProductSummaryResponse
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductNearbyProjection

object ProductMapper {

    fun ProductNearbyProjection.toSummaryResponse() = ProductSummaryResponse(
        id = id,
        title = title,
        price = price,
        productType = ProductType.valueOf(productType),
        status = ProductStatus.valueOf(status),
        popularityScore = popularityScore.toDouble(),
        distanceKm = distanceKm,
        merchantName = merchantName,
        shopAddress = shopAddress,
        shopLat = shopLat,
        shopLng = shopLng,
        category = category?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
    )

    fun ProductEntity.toDetailResponse(
        wishlistCount: Long,
        reservationCount: Long,
        purchaseCount: Long,
        images: List<ProductImageResponse> = emptyList(),
        menuOptions: List<ProductMenuOptionGroupResponse> = emptyList(),
        specs: List<ProductSpecItem>? = null,
    ) = ProductDetailResponse(
        id = id,
        title = title,
        description = description,
        price = price,
        productType = productType,
        status = status,
        stock = stock,
        availableFrom = availableFrom,
        availableUntil = availableUntil,
        shopLat = shopLat,
        shopLng = shopLng,
        shopAddress = merchant.shopAddress,
        merchantName = merchant.businessName,
        wishlistCount = wishlistCount,
        reservationCount = reservationCount,
        purchaseCount = purchaseCount,
        category = category,
        images = images,
        menuOptions = menuOptions,
        specs = specs,
    )

    fun ProductEntity.toListItem(wishlistCount: Long) = ProductListItem(
        id = id,
        title = title,
        price = price,
        status = status,
        productType = productType,
        stock = stock,
        wishlistCount = wishlistCount,
    )

    fun ProductEntity.toStatusResponse() = ProductStatusResponse(id = id, status = status)

    fun ProductEntity.toAdminItem() = AdminProductItem(
        productId = id,
        title = title,
        price = price,
        merchantId = merchant.userId,
        merchantName = merchant.businessName,
        status = status,
        createdAt = createdAt,
    )
}
