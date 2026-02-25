package com.nearpick.nearpick.transaction

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.reservation.ReservationService
import com.nearpick.domain.reservation.dto.ReservationCreateRequest
import com.nearpick.domain.reservation.dto.ReservationResponse
import com.nearpick.domain.reservation.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationServiceImpl(
    private val reservationRepository: ReservationRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) : ReservationService {

    @Transactional
    override fun create(userId: Long, request: ReservationCreateRequest): ReservationStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val product = productRepository.findById(requireNotNull(request.productId))
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE)
        }
        val reservation = reservationRepository.save(
            ReservationEntity(
                user = user,
                product = product,
                quantity = request.quantity,
                memo = request.memo,
                visitScheduledAt = request.visitAt,
            ),
        )
        return ReservationStatusResponse(id = reservation.id, status = reservation.status)
    }

    @Transactional(readOnly = true)
    override fun getMyList(userId: Long): List<ReservationResponse> =
        reservationRepository.findAllByUser_Id(userId).map { it.toResponse() }

    @Transactional
    override fun cancel(userId: Long, reservationId: Long): ReservationStatusResponse {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { BusinessException(ErrorCode.RESERVATION_NOT_FOUND) }
        if (reservation.user.id != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        if (reservation.status !in listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        reservation.status = ReservationStatus.CANCELLED
        return ReservationStatusResponse(id = reservation.id, status = reservation.status)
    }

    @Transactional(readOnly = true)
    override fun getMerchantList(merchantId: Long, status: ReservationStatus?): List<ReservationResponse> =
        reservationRepository.findByMerchant(merchantId, status).map { it.toResponse() }

    @Transactional
    override fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { BusinessException(ErrorCode.RESERVATION_NOT_FOUND) }
        if (reservation.product.merchant.userId != merchantId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        if (reservation.status != ReservationStatus.PENDING) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        reservation.status = ReservationStatus.CONFIRMED
        return ReservationStatusResponse(id = reservation.id, status = reservation.status)
    }

    private fun ReservationEntity.toResponse() = ReservationResponse(
        id = id,
        productId = product.id,
        productTitle = product.title,
        merchantName = product.merchant.businessName,
        status = status,
        visitAt = visitScheduledAt,
        quantity = quantity,
        memo = memo,
    )
}
