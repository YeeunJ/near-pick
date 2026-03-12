package com.nearpick.nearpick.product.entity

import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_location", columnList = "shop_lat, shop_lng"),
        Index(name = "idx_products_status_type", columnList = "status, product_type"),
        Index(name = "idx_products_merchant", columnList = "merchant_id"),
    ]
)
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    var merchant: MerchantProfileEntity,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var price: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    var productType: ProductType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ProductStatus = ProductStatus.DRAFT,

    @Column(nullable = false)
    var stock: Int = 0,

    @Column(name = "available_from")
    var availableFrom: LocalDateTime? = null,

    @Column(name = "available_until")
    var availableUntil: LocalDateTime? = null,

    @Column(name = "shop_lat", nullable = false, precision = 10, scale = 7)
    var shopLat: BigDecimal,

    @Column(name = "shop_lng", nullable = false, precision = 10, scale = 7)
    var shopLng: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    var category: ProductCategory? = null,

    @Column(name = "specs", columnDefinition = "TEXT")
    var specs: String? = null,

    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
