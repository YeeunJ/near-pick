package com.nearpick.domain.flashpurchase

import com.nearpick.domain.flashpurchase.dto.FlashPurchaseCreatedResponse
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseRequest
import com.nearpick.domain.flashpurchase.dto.FlashPurchaseResponse

interface FlashPurchaseService {
    fun purchase(userId: Long, request: FlashPurchaseRequest): FlashPurchaseCreatedResponse
    fun getMyList(userId: Long): List<FlashPurchaseResponse>
}
