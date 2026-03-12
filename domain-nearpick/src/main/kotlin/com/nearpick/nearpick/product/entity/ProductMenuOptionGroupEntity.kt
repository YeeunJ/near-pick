package com.nearpick.nearpick.product.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "product_menu_option_groups",
    indexes = [Index(name = "idx_menu_option_groups_product_id", columnList = "product_id")]
)
class ProductMenuOptionGroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false)
    var required: Boolean = false,

    @Column(name = "max_select", nullable = false)
    var maxSelect: Int = 1,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val choices: MutableList<ProductMenuChoiceEntity> = mutableListOf(),
)
