package com.nearpick.nearpick.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface WishlistRepository : JpaRepository<WishlistEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
}
