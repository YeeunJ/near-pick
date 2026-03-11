package com.nearpick.domain.product

import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductListItem
import com.nearpick.domain.product.dto.ProductNearbyRequest
import com.nearpick.domain.product.dto.ProductStatusResponse
import com.nearpick.domain.product.dto.ProductSummaryResponse
import org.springframework.data.domain.Page

interface ProductService {
    fun getNearby(request: ProductNearbyRequest, userId: Long? = null): Page<ProductSummaryResponse>
    fun getDetail(productId: Long): ProductDetailResponse
    fun create(merchantId: Long, request: ProductCreateRequest): ProductStatusResponse
    fun close(merchantId: Long, productId: Long): ProductStatusResponse
    fun getMyProducts(merchantId: Long, page: Int, size: Int): Page<ProductListItem>
}
