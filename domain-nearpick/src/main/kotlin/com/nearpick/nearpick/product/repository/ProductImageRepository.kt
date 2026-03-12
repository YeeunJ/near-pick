package com.nearpick.nearpick.product.repository

import com.nearpick.nearpick.product.entity.ProductImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImageEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrder(productId: Long): List<ProductImageEntity>
    fun countByProductId(productId: Long): Long
    fun findByIdAndProductId(id: Long, productId: Long): ProductImageEntity?
}
