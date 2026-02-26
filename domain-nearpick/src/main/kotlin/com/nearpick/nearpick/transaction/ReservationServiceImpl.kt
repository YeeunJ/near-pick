package com.nearpick.nearpick.transaction

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.transaction.ReservationService
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.transaction.TransactionMapper.toItem
import com.nearpick.nearpick.transaction.TransactionMapper.toStatusResponse
import com.nearpick.nearpick.user.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReservationServiceImpl(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) : ReservationService {

    @Transactional
    override fun create(
        userId: Long,
        productId: Long,
        request: ReservationCreateRequest,
    ): ReservationStatusResponse {
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        val product = productRepository.findById(productId).orElseThrow {
            BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE)
        }
        val reservation = ReservationEntity(
            user = user,
            product = product,
            quantity = request.quantity,
            memo = request.memo,
            visitScheduledAt = request.visitScheduledAt,
        )
        return reservationRepository.save(reservation).toStatusResponse()
    }

    @Transactional
    override fun cancel(userId: Long, reservationId: Long): ReservationStatusResponse {
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        }
        if (reservation.user.id != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        if (reservation.status != ReservationStatus.PENDING) {
            throw BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CANCELLED)
        }
        reservation.status = ReservationStatus.CANCELLED
        return reservation.toStatusResponse()
    }

    @Transactional
    override fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse {
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        }
        if (reservation.product.merchant.userId != merchantId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        if (reservation.status != ReservationStatus.PENDING) {
            throw BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CONFIRMED)
        }
        reservation.status = ReservationStatus.CONFIRMED
        return reservation.toStatusResponse()
    }

    override fun getMyReservations(userId: Long, page: Int, size: Int): Page<ReservationItem> =
        reservationRepository.findAllByUser_Id(userId, PageRequest.of(page, size))
            .map { it.toItem() }

    override fun getPendingReservations(merchantId: Long, page: Int, size: Int): Page<ReservationItem> =
        reservationRepository.findByMerchantIdAndStatus(
            merchantId = merchantId,
            status = ReservationStatus.PENDING,
            pageable = PageRequest.of(page, size),
        ).map { it.toItem() }
}

