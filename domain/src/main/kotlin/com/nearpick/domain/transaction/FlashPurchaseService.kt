package com.nearpick.domain.transaction

import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import org.springframework.data.domain.Page

interface FlashPurchaseService {
    fun purchase(userId: Long, productId: Long, request: FlashPurchaseCreateRequest): FlashPurchaseStatusResponse
    fun getMyPurchases(userId: Long, page: Int, size: Int): Page<FlashPurchaseItem>
}
