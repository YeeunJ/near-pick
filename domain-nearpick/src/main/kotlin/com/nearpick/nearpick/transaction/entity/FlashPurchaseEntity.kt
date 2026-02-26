package com.nearpick.nearpick.transaction.entity

import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.user.entity.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "flash_purchases",
    indexes = [Index(name = "idx_flash_purchases_user", columnList = "user_id")]
)
class FlashPurchaseEntity(
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
    var status: FlashPurchaseStatus = FlashPurchaseStatus.PENDING,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(name = "purchased_at", nullable = false, updatable = false)
    val purchasedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
