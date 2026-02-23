package com.nearpick.app.domain.transaction.entity

import com.nearpick.app.domain.product.entity.Product
import com.nearpick.app.domain.transaction.enums.FlashPurchaseStatus
import com.nearpick.app.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "flash_purchases",
    indexes = [Index(name = "idx_flash_purchases_user", columnList = "user_id")]
)
class FlashPurchase(
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
    var status: FlashPurchaseStatus = FlashPurchaseStatus.PENDING,

    @Column(name = "purchased_at", nullable = false, updatable = false)
    val purchasedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
