package com.nearpick.nearpick.user

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "merchant_profiles")
class MerchantProfileEntity(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    var user: UserEntity,

    @Column(name = "business_name", nullable = false, length = 100)
    var businessName: String,

    @Column(name = "business_reg_no", nullable = false, unique = true, length = 20)
    var businessRegNo: String,

    @Column(name = "shop_lat", nullable = false, precision = 10, scale = 7)
    var shopLat: BigDecimal,

    @Column(name = "shop_lng", nullable = false, precision = 10, scale = 7)
    var shopLng: BigDecimal,

    @Column(name = "shop_address", length = 255)
    var shopAddress: String? = null,

    @Column(precision = 3, scale = 2)
    var rating: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
