package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.WishlistService
import com.nearpick.domain.transaction.dto.WishlistItem
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toItem
import com.nearpick.nearpick.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.nearpick.nearpick.transaction.entity.WishlistEntity
import com.nearpick.nearpick.transaction.repository.WishlistRepository

@Service
@Transactional(readOnly = true)
class WishlistServiceImpl(
    private val wishlistRepository: WishlistRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) : WishlistService {

    @Transactional
    override fun add(userId: Long, productId: Long): Long {
        if (wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
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
        val wishlist = wishlistRepository.findByUser_IdAndProduct_Id(userId, productId)
            ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        wishlistRepository.delete(wishlist)
    }

    override fun getMyWishlists(userId: Long): List<WishlistItem> =
        wishlistRepository.findAllByUser_Id(userId).map { it.toItem() }
}
