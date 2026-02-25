package com.nearpick.domain.transaction

import com.nearpick.domain.transaction.dto.WishlistItem

interface WishlistService {
    fun add(userId: Long, productId: Long): Long
    fun remove(userId: Long, productId: Long)
    fun getMyWishlists(userId: Long): List<WishlistItem>
}
