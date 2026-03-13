package com.nearpick.nearpick.transaction.scheduler

import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ReservationScheduler(
    private val reservationRepository: ReservationRepository,
    private val productRepository: ProductRepository,
) {

    private val log = LoggerFactory.getLogger(ReservationScheduler::class.java)

    /** 매 시간 정각: CONFIRMED + visitScheduledAt+2h 초과 → NO_SHOW */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun processNoShow() {
        val threshold = LocalDateTime.now().minusHours(2)
        val targets = reservationRepository.findConfirmedExpiredForNoShow(threshold)
        if (targets.isEmpty()) return

        targets.forEach { it.status = ReservationStatus.NO_SHOW }
        log.info("[Scheduler] NO_SHOW processed: ${targets.size} reservations")
    }

    /** 매 시간 30분: PENDING + visitScheduledAt 초과 → CANCELLED + 재고 복원 */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    fun processExpiredPending() {
        val now = LocalDateTime.now()
        val targets = reservationRepository.findPendingExpired(now)
        if (targets.isEmpty()) return

        targets.forEach { reservation ->
            reservation.status = ReservationStatus.CANCELLED
            productRepository.incrementStock(reservation.product.id, reservation.quantity)
            productRepository.resumeIfRestored(reservation.product.id)
        }
        log.info("[Scheduler] Expired PENDING cancelled: ${targets.size} reservations")
    }
}
