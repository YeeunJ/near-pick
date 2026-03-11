package com.nearpick.nearpick.transaction.messaging

import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.user.repository.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Dead Letter Queue Consumer — flash-purchase-dlq 토픽 처리.
 * 정상 Consumer 에서 처리 실패한 이벤트를 수신하여 FAILED 상태로 기록.
 * 이력 보존 및 관리자 수동 조치를 위한 가시성 확보.
 */
@Component
class FlashPurchaseDlqConsumer(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val meterRegistry: MeterRegistry,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["flash-purchase-dlq"], groupId = "flash-purchase-dlq-cg")
    @Transactional
    fun consume(event: FlashPurchaseRequestEvent) {
        log.warn("[DLQ] flash-purchase 처리 실패 이벤트 수신: idempotencyKey={}, userId={}, productId={}",
            event.idempotencyKey, event.userId, event.productId)

        val user = userRepository.findById(event.userId).orElse(null)
        val product = productRepository.findById(event.productId).orElse(null)

        if (user == null || product == null) {
            log.error("[DLQ] 사용자 또는 상품 없음 — 레코드 저장 불가. userId={}, productId={}",
                event.userId, event.productId)
            meterRegistry.counter("flash.purchase.dlq", "result", "entity_not_found").increment()
            return
        }

        flashPurchaseRepository.save(
            FlashPurchaseEntity(
                user = user,
                product = product,
                quantity = event.quantity,
                status = FlashPurchaseStatus.FAILED,
            )
        )
        meterRegistry.counter("flash.purchase.dlq", "result", "recorded").increment()
        log.info("[DLQ] FAILED 상태로 기록 완료: userId={}, productId={}", event.userId, event.productId)
    }
}
