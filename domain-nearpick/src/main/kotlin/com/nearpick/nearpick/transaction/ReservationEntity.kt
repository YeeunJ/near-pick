package com.nearpick.nearpick.transaction

import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.nearpick.product.ProductEntity
import com.nearpick.nearpick.user.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reservations",
    indexes = [Index(name = "idx_reservations_user", columnList = "user_id")]
)
class ReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.PENDING,

    @Column(name = "visit_scheduled_at")
    var visitScheduledAt: LocalDateTime? = null,

    @Column(name = "reserved_at", nullable = false, updatable = false)
    val reservedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
