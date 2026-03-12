package com.nearpick.nearpick.transaction.messaging

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class LocalFlashPurchaseEventProducer : FlashPurchaseEventProducer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(event: FlashPurchaseRequestEvent) {
        log.info(
            "[local] FlashPurchase event received (Kafka disabled): " +
                "idempotencyKey={}, userId={}, productId={}, qty={}",
            event.idempotencyKey, event.userId, event.productId, event.quantity,
        )
    }
}
