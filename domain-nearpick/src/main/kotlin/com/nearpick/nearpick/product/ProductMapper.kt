package com.nearpick.nearpick.product

import com.nearpick.domain.admin.dto.AdminProductItem
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductListItem
import com.nearpick.domain.product.dto.ProductStatusResponse
import com.nearpick.domain.product.dto.ProductSummaryResponse

object ProductMapper {

    fun ProductNearbyProjection.toSummaryResponse() = ProductSummaryResponse(
        id = id,
        title = title,
        price = price,
        productType = ProductType.valueOf(productType),
        status = ProductStatus.valueOf(status),
        popularityScore = popularityScore,
        distanceKm = distanceKm,
        merchantName = merchantName,
    )

    fun ProductEntity.toDetailResponse(
        wishlistCount: Long,
        reservationCount: Long,
        purchaseCount: Long,
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
        merchantId = merchant.userId,
        merchantName = merchant.businessName,
        status = status,
        createdAt = createdAt,
    )
}
