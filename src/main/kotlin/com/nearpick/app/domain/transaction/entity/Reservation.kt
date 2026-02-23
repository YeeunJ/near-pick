package com.nearpick.app.domain.transaction.entity

import com.nearpick.app.domain.product.entity.Product
import com.nearpick.app.domain.transaction.enums.ReservationStatus
import com.nearpick.app.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reservations",
    indexes = [Index(name = "idx_reservations_user", columnList = "user_id")]
)
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

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
