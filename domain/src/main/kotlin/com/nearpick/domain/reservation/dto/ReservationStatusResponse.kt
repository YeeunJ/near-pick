package com.nearpick.domain.reservation.dto

import com.nearpick.domain.transaction.ReservationStatus

data class ReservationStatusResponse(
    val id: Long,
    val status: ReservationStatus,
)
