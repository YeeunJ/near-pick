package com.nearpick.nearpick.transaction.messaging

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class FlashPurchaseProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    fun send(event: FlashPurchaseRequestEvent) {
        // key = productId → 같은 파티션으로 라우팅 → 상품별 순서 보장
        kafkaTemplate.send("flash-purchase-requests", event.productId.toString(), event)
    }
}
