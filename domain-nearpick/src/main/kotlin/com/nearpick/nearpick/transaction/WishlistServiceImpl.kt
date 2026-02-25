package com.nearpick.nearpick.transaction

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.wishlist.WishlistService
import com.nearpick.domain.wishlist.dto.WishlistAddRequest
import com.nearpick.domain.wishlist.dto.WishlistAddedResponse
import com.nearpick.domain.wishlist.dto.WishlistItemResponse
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WishlistServiceImpl(
    private val wishlistRepository: WishlistRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) : WishlistService {

    @Transactional
    override fun add(userId: Long, request: WishlistAddRequest): WishlistAddedResponse {
        if (wishlistRepository.existsByUser_IdAndProduct_Id(userId, request.productId)) {
            throw BusinessException(ErrorCode.ALREADY_WISHLISTED)
        }
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = productRepository.findById(request.productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE)
        }
        val saved = wishlistRepository.save(WishlistEntity(user = user, product = product))
        return WishlistAddedResponse(productId = saved.product.id, addedAt = saved.createdAt)
    }

    @Transactional
    override fun remove(userId: Long, productId: Long) {
        val wishlist = wishlistRepository.findByUser_IdAndProduct_Id(userId, productId) ?: return
        wishlistRepository.delete(wishlist)
    }

    @Transactional(readOnly = true)
    override fun getMyList(userId: Long): List<WishlistItemResponse> =
        wishlistRepository.findAllByUser_Id(userId).map { w ->
            WishlistItemResponse(
                productId = w.product.id,
                title = w.product.title,
                price = w.product.price,
                merchantName = w.product.merchant.businessName,
                status = w.product.status,
            )
        }
}
