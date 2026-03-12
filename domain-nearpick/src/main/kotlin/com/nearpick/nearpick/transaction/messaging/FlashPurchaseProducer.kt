package com.nearpick.nearpick.transaction.messaging

import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@Profile("!local & !test")
class KafkaFlashPurchaseProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : FlashPurchaseEventProducer {

    override fun send(event: FlashPurchaseRequestEvent) {
        // key = productId → 같은 파티션으로 라우팅 → 상품별 순서 보장
        kafkaTemplate.send("flash-purchase-requests", event.productId.toString(), event)
    }
}
