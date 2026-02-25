package com.nearpick.nearpick.transaction

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.WishlistService
import com.nearpick.domain.transaction.dto.WishlistItem
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.transaction.TransactionMapper.toItem
import com.nearpick.nearpick.user.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WishlistServiceImpl(
    private val wishlistRepository: WishlistRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) : WishlistService {

    @Transactional
    override fun add(userId: Long, productId: Long): Long {
        if (wishlistRepository.existsByUser_UserIdAndProduct_Id(userId, productId)) {
            throw BusinessException(ErrorCode.ALREADY_WISHLISTED)
        }
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        val product = productRepository.findById(productId).orElseThrow {
            BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        return try {
            wishlistRepository.save(WishlistEntity(user = user, product = product)).id
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.ALREADY_WISHLISTED)
        }
    }

    @Transactional
    override fun remove(userId: Long, productId: Long) {
        val wishlist = wishlistRepository.findByUser_UserIdAndProduct_Id(userId, productId)
            ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        wishlistRepository.delete(wishlist)
    }

    override fun getMyWishlists(userId: Long): List<WishlistItem> =
        wishlistRepository.findAllByUser_UserId(userId).map { it.toItem() }
}
