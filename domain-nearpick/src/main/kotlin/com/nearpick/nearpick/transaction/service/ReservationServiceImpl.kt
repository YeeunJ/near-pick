package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.ReservationService
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.dto.ReservationDetailResponse
import com.nearpick.domain.transaction.dto.ReservationItem
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
import com.nearpick.domain.transaction.dto.ReservationVisitRequest
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toDetailResponse
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toItem
import com.nearpick.nearpick.transaction.mapper.TransactionMapper.toStatusResponse
import com.nearpick.nearpick.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.nearpick.nearpick.transaction.entity.ReservationEntity
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import java.time.LocalDateTime

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
        validateAvailability(product)

        if (product.productType == ProductType.RESERVATION) {
            val updated = productRepository.decrementStockIfSufficient(productId, request.quantity)
            if (updated == 0) throw BusinessException(ErrorCode.OUT_OF_STOCK)
            productRepository.pauseIfSoldOut(productId)
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
        productRepository.incrementStock(reservation.product.id, reservation.quantity)
        productRepository.resumeIfRestored(reservation.product.id)
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
        reservation.visitCode = generateCode()
        return reservationRepository.save(reservation).toStatusResponse()
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

    @Transactional
    override fun cancelByMerchant(merchantId: Long, reservationId: Long): ReservationStatusResponse {
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        }
        if (reservation.product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        if (reservation.status !in listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))
            throw BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CANCELLED)

        reservation.status = ReservationStatus.CANCELLED
        productRepository.incrementStock(reservation.product.id, reservation.quantity)
        productRepository.resumeIfRestored(reservation.product.id)
        return reservationRepository.save(reservation).toStatusResponse()
    }

    @Transactional
    override fun visitByCode(merchantId: Long, request: ReservationVisitRequest): ReservationStatusResponse {
        val reservation = reservationRepository.findByVisitCode(request.code)
            ?: throw BusinessException(ErrorCode.RESERVATION_VISIT_CODE_INVALID)
        if (reservation.product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        if (reservation.status != ReservationStatus.CONFIRMED)
            throw BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CONFIRMED)

        reservation.status = ReservationStatus.COMPLETED
        reservation.completedAt = LocalDateTime.now()
        reservation.visitCode = null
        return reservationRepository.save(reservation).toStatusResponse()
    }

    override fun getDetail(userId: Long, reservationId: Long): ReservationDetailResponse {
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        }
        val isOwner = reservation.user.id == userId
        val isMerchant = reservation.product.merchant.userId == userId
        if (!isOwner && !isMerchant) throw BusinessException(ErrorCode.FORBIDDEN)
        return reservation.toDetailResponse(isOwner = isOwner)
    }

    override fun getMerchantReservations(
        merchantId: Long,
        status: ReservationStatus?,
        page: Int,
        size: Int,
    ): Page<ReservationItem> =
        reservationRepository.findByMerchantIdAndOptionalStatus(
            merchantId = merchantId,
            status = status,
            pageable = PageRequest.of(page, size),
        ).map { it.toItem() }

    private fun generateCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun validateAvailability(product: com.nearpick.nearpick.product.entity.ProductEntity) {
        val now = LocalDateTime.now()
        if (product.availableFrom != null && now.isBefore(product.availableFrom))
            throw BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE_YET)
        if (product.availableUntil != null && now.isAfter(product.availableUntil))
            throw BusinessException(ErrorCode.PRODUCT_AVAILABILITY_EXPIRED)
    }
}

