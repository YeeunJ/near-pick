package com.nearpick.app.domain.transaction.entity

import com.nearpick.app.domain.product.entity.Product
import com.nearpick.app.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = [UniqueConstraint(name = "uq_wishlist_user_product", columnNames = ["user_id", "product_id"])],
    indexes = [Index(name = "idx_wishlists_user", columnList = "user_id")]
)
class Wishlist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
