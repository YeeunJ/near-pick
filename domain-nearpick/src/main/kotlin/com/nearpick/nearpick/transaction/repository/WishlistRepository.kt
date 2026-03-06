package com.nearpick.nearpick.transaction.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import com.nearpick.nearpick.transaction.entity.WishlistEntity

interface WishlistRepository : JpaRepository<WishlistEntity, Long> {

    fun existsByUser_IdAndProduct_Id(userId: Long, productId: Long): Boolean

    fun findByUser_IdAndProduct_Id(userId: Long, productId: Long): WishlistEntity?

    fun countByProduct_Id(productId: Long): Long

    fun findTop200ByUser_IdOrderByCreatedAtDesc(userId: Long): List<WishlistEntity>

    /** Batch wishlist count per product — avoids N+1 in product listing */
    @Query("""
        SELECT w.product.id AS productId, COUNT(w) AS cnt
        FROM WishlistEntity w
        WHERE w.product.id IN :productIds
        GROUP BY w.product.id
    """)
    fun countByProductIds(@Param("productIds") productIds: List<Long>): List<WishlistCountRow>
}

interface WishlistCountRow {
    val productId: Long
    val cnt: Long
}
