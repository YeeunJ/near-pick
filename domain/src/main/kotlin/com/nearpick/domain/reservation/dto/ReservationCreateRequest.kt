package com.nearpick.domain.reservation.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ReservationCreateRequest(
    // Long? 로 선언해야 @NotNull이 Bean Validation에서 동작함
    // (non-nullable이면 역직렬화 실패 시 ValidationException 대신 HttpMessageNotReadableException 발생)
    @field:NotNull val productId: Long?,
    @field:NotNull val visitAt: LocalDateTime?,
    @field:Min(1) val quantity: Int = 1,
    val memo: String? = null,
)
