package com.nearpick.nearpick.product.repository

import com.nearpick.nearpick.product.entity.ProductMenuOptionGroupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductMenuOptionGroupRepository : JpaRepository<ProductMenuOptionGroupEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrder(productId: Long): List<ProductMenuOptionGroupEntity>
    fun deleteAllByProductId(productId: Long)
    fun findByIdAndProductId(id: Long, productId: Long): ProductMenuOptionGroupEntity?
}
