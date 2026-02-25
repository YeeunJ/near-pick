package com.nearpick.nearpick.transaction

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.flashpurchase.FlashPurchaseService
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseCreatedResponse
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseRequest
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseResponse
import com.nearpick.domain.product.ProductStatus
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlashPurchaseServiceImpl(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) : FlashPurchaseService {

    @Transactional
    override fun purchase(userId: Long, request: FlashPurchaseRequest): FlashPurchaseCreatedResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = productRepository.findById(request.productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE)
        }
        // NOTE: stock decrement is not concurrency-safe here.
        // Production should use pessimistic locking (@Lock(PESSIMISTIC_WRITE)) or
        // an optimistic-lock retry loop to prevent overselling.
        if (product.stock < request.quantity) {
            throw BusinessException(ErrorCode.OUT_OF_STOCK)
        }
        product.stock -= request.quantity
        val purchase = flashPurchaseRepository.save(
            FlashPurchaseEntity(user = user, product = product, quantity = request.quantity),
        )
        return FlashPurchaseCreatedResponse(
            id = purchase.id,
            status = purchase.status,
            purchasedAt = purchase.purchasedAt,
        )
    }

    @Transactional(readOnly = true)
    override fun getMyList(userId: Long): List<FlashPurchaseResponse> =
        flashPurchaseRepository.findAllByUser_Id(userId).map { p ->
            FlashPurchaseResponse(
                id = p.id,
                productTitle = p.product.title,
                merchantName = p.product.merchant.businessName,
                status = p.status,
                quantity = p.quantity,
                purchasedAt = p.purchasedAt,
            )
        }
}
