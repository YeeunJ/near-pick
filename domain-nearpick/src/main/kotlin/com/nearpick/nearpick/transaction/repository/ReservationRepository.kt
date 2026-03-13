package com.nearpick.nearpick.transaction.repository

import com.nearpick.domain.transaction.ReservationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import com.nearpick.nearpick.transaction.entity.ReservationEntity

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {

    fun findAllByUser_Id(userId: Long, pageable: Pageable): Page<ReservationEntity>

    fun countByProduct_Id(productId: Long): Long

    /** JPQL with @Param to avoid hardcoded FQN — safe across package refactors */
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
          AND r.status = :status
        ORDER BY r.reservedAt DESC
    """)
    fun findByMerchantIdAndStatus(
        @Param("merchantId") merchantId: Long,
        @Param("status") status: ReservationStatus,
        pageable: Pageable,
    ): Page<ReservationEntity>

    @Query("""
        SELECT COUNT(r)
        FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
          AND r.reservedAt >= :from
          AND r.reservedAt < :to
    """)
    fun countByMerchantIdAndPeriod(
        @Param("merchantId") merchantId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long

    @Query("""
        SELECT COUNT(r)
        FROM ReservationEntity r
        WHERE r.product.id = :productId
          AND r.status = :status
    """)
    fun countByProductIdAndStatus(
        @Param("productId") productId: Long,
        @Param("status") status: ReservationStatus,
    ): Long

    /** 방문 코드로 예약 조회 */
    fun findByVisitCode(visitCode: String): ReservationEntity?

    /** 소상공인 예약 목록 - 상태 필터 (null이면 전체) */
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.product.merchant.userId = :merchantId
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reservedAt DESC
    """)
    fun findByMerchantIdAndOptionalStatus(
        @Param("merchantId") merchantId: Long,
        @Param("status") status: ReservationStatus?,
        pageable: Pageable,
    ): Page<ReservationEntity>

    /** 스케줄러: NO_SHOW 대상 (CONFIRMED + visitScheduledAt+2h 초과) */
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.status = 'CONFIRMED'
        AND r.visitScheduledAt IS NOT NULL
        AND r.visitScheduledAt < :threshold
    """)
    fun findConfirmedExpiredForNoShow(
        @Param("threshold") threshold: LocalDateTime,
    ): List<ReservationEntity>

    /** 스케줄러: 만료 PENDING 대상 (PENDING + visitScheduledAt 초과) */
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.status = 'PENDING'
        AND r.visitScheduledAt IS NOT NULL
        AND r.visitScheduledAt < :now
    """)
    fun findPendingExpired(
        @Param("now") now: LocalDateTime,
    ): List<ReservationEntity>
}
