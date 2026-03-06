package com.nearpick.nearpick.transaction.messaging

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.user.repository.UserRepository
import org.redisson.api.RedissonClient
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class FlashPurchaseConsumer(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val redissonClient: RedissonClient,
) {

    @KafkaListener(topics = ["flash-purchase-requests"], groupId = "flash-purchase-cg")
    @Transactional
    fun consume(event: FlashPurchaseRequestEvent) {
        // 1. Idempotency 체크 (Redisson RBucket SETNX) — 중복 요청 방지
        val idempotencyKey = "idempotency:flash:${event.idempotencyKey}"
        val idempotencyBucket = redissonClient.getBucket<String>(idempotencyKey)
        val isNew = idempotencyBucket.setIfAbsent("1", Duration.ofDays(1))
        if (!isNew) return

        // 2. Distributed Lock 획득 (Redisson)
        val lock = redissonClient.getLock("lock:flash:product:${event.productId}")
        val acquired = lock.tryLock(3, 10, TimeUnit.SECONDS)
        if (!acquired) {
            idempotencyBucket.delete()  // 재시도 가능하도록 키 삭제
            throw BusinessException(ErrorCode.FLASH_PURCHASE_LOCK_FAILED)
        }

        try {
            // 3. 재고 확인 및 감소 (DB Pessimistic Lock)
            val product = productRepository.findByIdWithLock(event.productId)
                ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            if (product.stock < event.quantity) {
                throw BusinessException(ErrorCode.OUT_OF_STOCK)
            }
            product.stock -= event.quantity

            // 4. 구매자 조회
            val user = userRepository.findById(event.userId).orElseThrow {
                BusinessException(ErrorCode.USER_NOT_FOUND)
            }

            // 5. FlashPurchase 엔티티 저장 (CONFIRMED)
            flashPurchaseRepository.save(
                FlashPurchaseEntity(
                    user = user,
                    product = product,
                    quantity = event.quantity,
                    status = FlashPurchaseStatus.CONFIRMED,
                )
            )
        } catch (e: BusinessException) {
            // 비즈니스 예외(재고부족 등)는 idempotency key 유지 (중복 재시도 방지)
            if (e.errorCode == ErrorCode.FLASH_PURCHASE_LOCK_FAILED) throw e
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }
}
