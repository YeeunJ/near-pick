package com.nearpick.domain.transaction

import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import org.springframework.data.domain.Page

interface ReservationService {
    fun create(userId: Long, productId: Long, request: ReservationCreateRequest): ReservationStatusResponse
    fun cancel(userId: Long, reservationId: Long): ReservationStatusResponse
    fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse
    fun getMyReservations(userId: Long, page: Int, size: Int): Page<ReservationItem>
    fun getPendingReservations(merchantId: Long, page: Int, size: Int): Page<ReservationItem>
}
