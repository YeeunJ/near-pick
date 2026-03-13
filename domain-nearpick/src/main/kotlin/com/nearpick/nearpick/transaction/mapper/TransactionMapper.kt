package com.nearpick.nearpick.transaction.mapper

import com.nearpick.domain.transaction.dto.FlashPurchaseDetailResponse
import com.nearpick.domain.transaction.dto.FlashPurchaseItem
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
import com.nearpick.domain.transaction.dto.ReservationDetailResponse
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.dto.WishlistItem
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.entity.ReservationEntity
import com.nearpick.nearpick.transaction.entity.WishlistEntity

object TransactionMapper {

    fun WishlistEntity.toItem() = WishlistItem(
        wishlistId = id,
        productId = product.id,
        productTitle = product.title,
        productPrice = product.price,
        productType = product.productType,
        productStatus = product.status,
        shopAddress = product.merchant.shopAddress,
        createdAt = createdAt,
    )

    fun ReservationEntity.toItem() = ReservationItem(
        reservationId = id,
        productId = product.id,
        productTitle = product.title,
        quantity = quantity,
        status = status,
        memo = memo,
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

    fun ReservationEntity.toDetailResponse(isOwner: Boolean) = ReservationDetailResponse(
        reservationId = id,
        productId = product.id,
        productTitle = product.title,
        quantity = quantity,
        status = status,
        memo = memo,
        visitScheduledAt = visitScheduledAt,
        reservedAt = reservedAt,
        completedAt = completedAt,
        visitCode = if (isOwner) visitCode else null,
    )

    fun FlashPurchaseEntity.toDetailResponse(isOwner: Boolean) = FlashPurchaseDetailResponse(
        purchaseId = id,
        productId = product.id,
        productTitle = product.title,
        quantity = quantity,
        status = status,
        purchasedAt = purchasedAt,
        pickedUpAt = pickedUpAt,
        pickupCode = if (isOwner) pickupCode else null,
    )
}
