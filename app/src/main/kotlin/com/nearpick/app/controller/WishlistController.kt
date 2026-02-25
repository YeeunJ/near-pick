package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.wishlist.WishlistService
import com.nearpick.domain.wishlist.dto.WishlistAddRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wishlists")
class WishlistController(
    private val wishlistService: WishlistService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: WishlistAddRequest,
    ) = ApiResponse.success(wishlistService.add(userId, request))

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ) {
        wishlistService.remove(userId, productId)
    }

    @GetMapping("/me")
    fun getMyList(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(wishlistService.getMyList(userId))
}
