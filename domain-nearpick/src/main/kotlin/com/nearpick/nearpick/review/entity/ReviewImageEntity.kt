package com.nearpick.nearpick.review.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "review_images",
    indexes = [Index(name = "idx_review_images_review_id", columnList = "review_id")]
)
class ReviewImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    val review: ReviewEntity,

    @Column(name = "s3_key", nullable = false, length = 500)
    val s3Key: String,

    @Column(name = "image_url", nullable = false, length = 1000)
    val imageUrl: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
)
