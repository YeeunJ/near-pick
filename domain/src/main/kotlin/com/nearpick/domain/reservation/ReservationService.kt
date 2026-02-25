package com.nearpick.domain.reservation

import com.nearpick.domain.reservation.dto.ReservationCreateRequest
import com.nearpick.domain.reservation.dto.ReservationResponse
import com.nearpick.domain.reservation.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.ReservationStatus

interface ReservationService {
    fun create(userId: Long, request: ReservationCreateRequest): ReservationStatusResponse
    fun getMyList(userId: Long): List<ReservationResponse>
    fun cancel(userId: Long, reservationId: Long): ReservationStatusResponse
    fun getMerchantList(merchantId: Long, status: ReservationStatus?): List<ReservationResponse>
    fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse
}
