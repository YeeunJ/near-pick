package com.nearpick.nearpick.product.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_menu_choices")
class ProductMenuChoiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ProductMenuOptionGroupEntity,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "additional_price", nullable = false)
    var additionalPrice: Int = 0,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
)
