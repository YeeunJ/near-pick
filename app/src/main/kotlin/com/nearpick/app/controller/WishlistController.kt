package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.WishlistService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wishlists")
@PreAuthorize("hasRole('CONSUMER')")
class WishlistController(private val wishlistService: WishlistService) {

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) = ApiResponse.success(mapOf("wishlistId" to wishlistService.add(userId, productId)))

    @DeleteMapping("/{productId}")
    fun remove(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        wishlistService.remove(userId, productId)
        return ApiResponse.success(Unit)
    }

    @GetMapping
    fun getMyWishlists(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(wishlistService.getMyWishlists(userId))
}
