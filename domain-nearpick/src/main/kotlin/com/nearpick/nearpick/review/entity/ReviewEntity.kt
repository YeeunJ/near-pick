package com.nearpick.nearpick.review.entity

import com.nearpick.domain.review.ReviewStatus
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.user.entity.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reviews",
    indexes = [
        Index(name = "idx_reviews_product_id", columnList = "product_id, status"),
        Index(name = "idx_reviews_user_id", columnList = "user_id"),
        Index(name = "idx_reviews_report_count", columnList = "report_count"),
    ]
)
class ReviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(name = "reservation_id", unique = true)
    val reservationId: Long? = null,

    @Column(name = "flash_purchase_id", unique = true)
    val flashPurchaseId: Long? = null,

    @Column(nullable = false)
    val rating: Int,

    @Column(nullable = false, length = 500)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReviewStatus = ReviewStatus.ACTIVE,

    @Column(name = "ai_checked", nullable = false)
    var aiChecked: Boolean = false,

    @Column(name = "ai_result", length = 20)
    var aiResult: String? = null,

    @Column(name = "blinded_reason", length = 100)
    var blindedReason: String? = null,

    @Column(name = "blind_pending", nullable = false)
    var blindPending: Boolean = false,

    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
