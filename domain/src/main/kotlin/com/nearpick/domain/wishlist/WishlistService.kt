package com.nearpick.domain.wishlist

import com.nearpick.domain.wishlist.dto.WishlistAddRequest
import com.nearpick.domain.wishlist.dto.WishlistAddedResponse
import com.nearpick.domain.wishlist.dto.WishlistItemResponse

interface WishlistService {
    fun add(userId: Long, request: WishlistAddRequest): WishlistAddedResponse
    fun remove(userId: Long, productId: Long)
    fun getMyList(userId: Long): List<WishlistItemResponse>
}
