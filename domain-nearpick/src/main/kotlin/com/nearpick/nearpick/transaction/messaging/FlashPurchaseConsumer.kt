package com.nearpick.nearpick.transaction.messaging

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.user.repository.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import org.redisson.api.RAtomicLong
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
@Profile("!test")
class FlashPurchaseConsumer(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val redissonClient: RedissonClient,
    private val meterRegistry: MeterRegistry,
) {

    @KafkaListener(topics = ["flash-purchase-requests"], groupId = "flash-purchase-cg")
    @Transactional
    fun consume(event: FlashPurchaseRequestEvent) {
        // 1. Idempotency 체크 — 중복 요청 방지 (SETNX)
        val idempotencyBucket = redissonClient.getBucket<String>("idempotency:flash:${event.idempotencyKey}")
        if (!idempotencyBucket.setIfAbsent("1", Duration.ofDays(1))) {
            meterRegistry.counter("flash.purchase", "result", "duplicate").increment()
            return
        }

        // 2. Redis 원자적 재고 차감 (분산 락 제거 — Redis 단일 스레드 원자성 활용)
        val stockCounter = ensureStockCounter(event.productId)
            ?: return  // PRODUCT_NOT_FOUND — idempotency key 유지 (재시도 방지)

        val remaining = stockCounter.addAndGet(-event.quantity.toLong())
        if (remaining < 0) {
            stockCounter.addAndGet(event.quantity.toLong())  // 복원
            meterRegistry.counter("flash.purchase", "result", "out_of_stock").increment()
            return  // OUT_OF_STOCK — idempotency key 유지 (재시도 방지)
        }

        try {
            // 3. DB 재고 차감 (비관적 락 없이 — Redis 원자성으로 이미 보호됨)
            val updated = productRepository.decrementStockIfSufficient(event.productId, event.quantity)
            if (updated == 0) {
                // DB-Redis 재고 불일치 (관리자 수동 조정 필요)
                stockCounter.addAndGet(event.quantity.toLong())
                return
            }

            // 4. 구매자 조회
            val user = userRepository.findById(event.userId).orElseThrow {
                BusinessException(ErrorCode.USER_NOT_FOUND)
            }

            // 5. 구매 엔티티용 상품 참조 (락 없이)
            val product = productRepository.findById(event.productId).orElseThrow {
                BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            }

            // 6. FlashPurchase 저장 (CONFIRMED) + pickupCode 생성
            val chars = ('A'..'Z') + ('0'..'9')
            val pickupCode = (1..6).map { chars.random() }.joinToString("")
            flashPurchaseRepository.save(
                FlashPurchaseEntity(
                    user = user,
                    product = product,
                    quantity = event.quantity,
                    status = FlashPurchaseStatus.CONFIRMED,
                    pickupCode = pickupCode,
                )
            )

            // 7. stock=0이면 상품 PAUSED 자동 전환
            productRepository.pauseIfSoldOut(event.productId)
            meterRegistry.counter("flash.purchase", "result", "success").increment()
        } catch (e: Exception) {
            stockCounter.addAndGet(event.quantity.toLong())  // Redis 재고 복원
            throw e
        }
    }

    /**
     * 상품 재고 Redis 카운터를 지연 초기화한다.
     * 최초 요청 시에만 DB 조회 후 초기화 — 이후 요청은 Redis 원자적 연산만 사용.
     * null 반환 시 상품 없음 (PRODUCT_NOT_FOUND).
     */
    private fun ensureStockCounter(productId: Long): RAtomicLong? {
        val stockCounter = redissonClient.getAtomicLong("stock:flash:$productId")
        if (stockCounter.isExists) return stockCounter

        // 초기화 락 — 최초 1회만 DB 조회
        val initLock = redissonClient.getLock("init:stock:flash:$productId")
        if (!initLock.tryLock(1, 5, TimeUnit.SECONDS)) return stockCounter  // 타 스레드 초기화 대기

        try {
            if (!stockCounter.isExists) {
                val product = productRepository.findById(productId).orElse(null) ?: return null
                stockCounter.set(product.stock.toLong())
                stockCounter.expire(Duration.ofHours(24))
            }
        } finally {
            if (initLock.isHeldByCurrentThread) initLock.unlock()
        }
        return stockCounter
    }
}
