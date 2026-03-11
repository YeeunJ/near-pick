package com.nearpick.nearpick.location.entity

import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "saved_locations",
    indexes = [Index(name = "idx_saved_location_consumer_id", columnList = "consumer_id")]
)
class SavedLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", nullable = false)
    val consumer: ConsumerProfileEntity,

    @Column(nullable = false, length = 50)
    var label: String,

    @Column(precision = 10, scale = 7, nullable = false)
    var lat: BigDecimal,

    @Column(precision = 10, scale = 7, nullable = false)
    var lng: BigDecimal,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
