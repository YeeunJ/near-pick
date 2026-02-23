package com.nearpick.app.domain.product.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "popularity_scores",
    indexes = [Index(name = "idx_popularity_score", columnList = "score DESC")]
)
class PopularityScore(
    @Id
    val productId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    var product: Product,

    // score = (view_count × 1) + (wishlist_count × 3) + (purchase_count × 5)
    @Column(nullable = false, precision = 10, scale = 4)
    var score: BigDecimal = BigDecimal.ZERO,

    @Column(name = "view_weight", nullable = false)
    var viewWeight: Int = 0,

    @Column(name = "wishlist_weight", nullable = false)
    var wishlistWeight: Int = 0,

    @Column(name = "purchase_weight", nullable = false)
    var purchaseWeight: Int = 0,

    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: LocalDateTime = LocalDateTime.now(),
)
