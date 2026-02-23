package com.nearpick.app.domain.user.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "consumer_profiles")
class ConsumerProfile(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    var user: User,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(name = "current_lat", precision = 10, scale = 7)
    var currentLat: BigDecimal? = null,

    @Column(name = "current_lng", precision = 10, scale = 7)
    var currentLng: BigDecimal? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
