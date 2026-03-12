package com.nearpick.nearpick.transaction.messaging

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpFlashPurchaseEventProducer : FlashPurchaseEventProducer {
    override fun send(event: FlashPurchaseRequestEvent) {
        // no-op for test profile
    }
}
