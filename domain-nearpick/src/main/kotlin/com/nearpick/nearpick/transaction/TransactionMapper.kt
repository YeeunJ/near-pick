package com.nearpick.nearpick.transaction

import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.dto.WishlistItem

object TransactionMapper {

    fun WishlistEntity.toItem() = WishlistItem(
        wishlistId = id,
        productId = product.id,
        productTitle = product.title,
        productPrice = product.price,
        productType = product.productType,
        createdAt = createdAt,
    )

    fun ReservationEntity.toItem() = ReservationItem(
        reservationId = id,
        productId = product.id,
        productTitle = product.title,
        quantity = quantity,
        status = status,
        visitScheduledAt = visitScheduledAt,
        reservedAt = reservedAt,
    )

    fun ReservationEntity.toStatusResponse() =
        ReservationStatusResponse(reservationId = id, status = status)

    fun FlashPurchaseEntity.toItem() = FlashPurchaseItem(
        purchaseId = id,
        productId = product.id,
        productTitle = product.title,
        quantity = quantity,
        status = status,
        purchasedAt = purchasedAt,
    )

    fun FlashPurchaseEntity.toStatusResponse() =
        FlashPurchaseStatusResponse(purchaseId = id, status = status)
}
