package com.nearpick.nearpick.review.entity

import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "review_replies")
class ReviewReplyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    val review: ReviewEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    val merchant: MerchantProfileEntity,

    @Column(nullable = false, length = 300)
    var content: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
