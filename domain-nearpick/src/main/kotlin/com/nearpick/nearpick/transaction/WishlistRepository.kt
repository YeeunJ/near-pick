package com.nearpick.nearpick.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface WishlistRepository : JpaRepository<WishlistEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
    fun existsByUser_IdAndProduct_Id(userId: Long, productId: Long): Boolean
    fun findByUser_IdAndProduct_Id(userId: Long, productId: Long): WishlistEntity?
    fun findAllByUser_Id(userId: Long): List<WishlistEntity>
}
