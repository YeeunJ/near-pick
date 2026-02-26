package com.nearpick.nearpick.transaction.entity

import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.user.entity.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = [UniqueConstraint(name = "uq_wishlist_user_product", columnNames = ["user_id", "product_id"])],
    indexes = [Index(name = "idx_wishlists_user", columnList = "user_id")]
)
class WishlistEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
