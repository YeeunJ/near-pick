package com.nearpick.domain.reservation.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ReservationCreateRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val visitAt: LocalDateTime,
    @field:Min(1) val quantity: Int = 1,
    val memo: String? = null,
)
