package com.nearpick.nearpick.transaction.messaging

interface FlashPurchaseEventProducer {
    fun send(event: FlashPurchaseRequestEvent)
}
