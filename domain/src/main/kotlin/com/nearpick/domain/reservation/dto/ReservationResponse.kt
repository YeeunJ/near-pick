package com.nearpick.domain.reservation.dto

import com.nearpick.domain.transaction.ReservationStatus
import java.time.LocalDateTime

data class ReservationResponse(
    val id: Long,
    val productId: Long,
    val productTitle: String,
    val merchantName: String,
    val status: ReservationStatus,
    val visitAt: LocalDateTime?,
    val quantity: Int,
    val memo: String?,
)
