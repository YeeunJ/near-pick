package com.nearpick.nearpick.product.scheduler

import com.nearpick.nearpick.product.repository.ProductRepository
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ProductScheduler(
    private val productRepository: ProductRepository,
    private val redissonClient: RedissonClient,
) {

    private val log = LoggerFactory.getLogger(ProductScheduler::class.java)

    /** 매 시간 정각: availableUntil 만료된 ACTIVE 상품 → PAUSED */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun pauseExpiredProducts() {
        val count = productRepository.pauseExpiredProducts(LocalDateTime.now())
        if (count > 0) log.info("[Scheduler] Paused $count expired products")
    }

    /** 매일 04:15: Redis↔DB 재고 일관성 검사 및 복원 */
    @Scheduled(cron = "0 15 4 * * *")
    fun syncRedisStockWithDb() {
        val keys = redissonClient.keys.getKeysByPattern("stock:flash:*")
        var mismatchCount = 0
        keys.forEach { key ->
            val productId = key.removePrefix("stock:flash:").toLongOrNull() ?: return@forEach
            val redisStock = redissonClient.getAtomicLong(key).get()
            val dbStock = productRepository.findById(productId).map { it.stock.toLong() }.orElse(null)
                ?: return@forEach

            if (redisStock != dbStock) {
                log.warn("[StockSync] productId=$productId Redis=$redisStock DB=$dbStock → Reset to DB value")
                redissonClient.getAtomicLong(key).set(dbStock)
                mismatchCount++
            }
        }
        if (mismatchCount > 0) {
            log.warn("[StockSync] Corrected $mismatchCount Redis stock mismatches")
        } else {
            log.info("[StockSync] All Redis stocks are consistent with DB")
        }
    }
}
