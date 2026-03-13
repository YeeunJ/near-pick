package com.nearpick.domain.transaction

import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchaseDetailResponse
import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchasePickupRequest
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import org.springframework.data.domain.Page

interface FlashPurchaseService {
    fun purchase(userId: Long, productId: Long, request: FlashPurchaseCreateRequest): FlashPurchaseStatusResponse
    fun getMyPurchases(userId: Long, page: Int, size: Int): Page<FlashPurchaseItem>
    fun pickupByCode(merchantId: Long, request: FlashPurchasePickupRequest): FlashPurchaseStatusResponse
    fun cancelByMerchant(merchantId: Long, purchaseId: Long): FlashPurchaseStatusResponse
    fun getDetail(userId: Long, purchaseId: Long): FlashPurchaseDetailResponse
    fun getMerchantPurchases(merchantId: Long, status: FlashPurchaseStatus?, page: Int, size: Int): Page<FlashPurchaseItem>
}
