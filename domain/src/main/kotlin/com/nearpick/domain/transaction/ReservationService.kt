package com.nearpick.domain.transaction

import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.dto.ReservationDetailResponse
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.dto.ReservationVisitRequest
import org.springframework.data.domain.Page

interface ReservationService {
    fun create(userId: Long, productId: Long, request: ReservationCreateRequest): ReservationStatusResponse
    fun cancel(userId: Long, reservationId: Long): ReservationStatusResponse
    fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse
    fun getMyReservations(userId: Long, page: Int, size: Int): Page<ReservationItem>
    fun getPendingReservations(merchantId: Long, page: Int, size: Int): Page<ReservationItem>
    fun cancelByMerchant(merchantId: Long, reservationId: Long): ReservationStatusResponse
    fun visitByCode(merchantId: Long, request: ReservationVisitRequest): ReservationStatusResponse
    fun getDetail(userId: Long, reservationId: Long): ReservationDetailResponse
    fun getMerchantReservations(merchantId: Long, status: ReservationStatus?, page: Int, size: Int): Page<ReservationItem>
}
