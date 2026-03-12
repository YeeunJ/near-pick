package com.nearpick.nearpick.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "product_images",
    indexes = [Index(name = "idx_product_images_product_id", columnList = "product_id")]
)
class ProductImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(name = "s3_key", nullable = false, length = 500)
    val s3Key: String,

    @Column(nullable = false, length = 1000)
    val url: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
