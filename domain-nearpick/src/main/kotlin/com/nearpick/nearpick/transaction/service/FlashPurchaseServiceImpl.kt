package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toItem
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseEventProducer
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseRequestEvent
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class FlashPurchaseServiceImpl(
    private val producer: FlashPurchaseEventProducer,
    private val flashPurchaseRepository: FlashPurchaseRepository,
) : FlashPurchaseService {

    @CircuitBreaker(name = "flashPurchase", fallbackMethod = "purchaseFallback")
    override fun purchase(
        userId: Long,
        productId: Long,
        request: FlashPurchaseCreateRequest,
    ): FlashPurchaseStatusResponse {
        // 시간 단위 idempotency key — 동일 사용자가 같은 상품을 1시간 내 중복 요청 방지
        val idempotencyKey = "$userId-$productId-${Instant.now().epochSecond / 3_600}"

        producer.send(
            FlashPurchaseRequestEvent(
                idempotencyKey = idempotencyKey,
                userId = userId,
                productId = productId,
                quantity = request.quantity,
            )
        )

        // Kafka 비동기 처리 — 즉시 PENDING 반환
        return FlashPurchaseStatusResponse(
            purchaseId = null,
            status = FlashPurchaseStatus.PENDING,
        )
    }

    @Suppress("unused")
    fun purchaseFallback(
        userId: Long,
        productId: Long,
        request: FlashPurchaseCreateRequest,
        ex: Exception,
    ): FlashPurchaseStatusResponse {
        throw BusinessException(ErrorCode.FLASH_PURCHASE_UNAVAILABLE)
    }

    override fun getMyPurchases(userId: Long, page: Int, size: Int): Page<FlashPurchaseItem> =
        flashPurchaseRepository.findAllByUser_Id(userId, PageRequest.of(page, size))
            .map { it.toItem() }
}
