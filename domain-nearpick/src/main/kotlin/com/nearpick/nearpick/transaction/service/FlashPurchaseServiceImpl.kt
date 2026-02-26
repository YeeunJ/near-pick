package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toItem
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toStatusResponse
import com.nearpick.nearpick.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository

@Service
@Transactional(readOnly = true)
class FlashPurchaseServiceImpl(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) : FlashPurchaseService {

    @Transactional
    override fun purchase(
        userId: Long,
        productId: Long,
        request: FlashPurchaseCreateRequest,
    ): FlashPurchaseStatusResponse {
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        // Pessimistic write lock — prevents concurrent oversell
        val product = productRepository.findByIdWithLock(productId)
            ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)

        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE)
        }
        if (product.stock < request.quantity) {
            throw BusinessException(ErrorCode.OUT_OF_STOCK)
        }

        product.stock -= request.quantity

        val purchase = FlashPurchaseEntity(
            user = user,
            product = product,
            quantity = request.quantity,
        )
        return flashPurchaseRepository.save(purchase).toStatusResponse()
    }

    override fun getMyPurchases(userId: Long, page: Int, size: Int): Page<FlashPurchaseItem> =
        flashPurchaseRepository.findAllByUser_Id(userId, PageRequest.of(page, size))
            .map { it.toItem() }
}
