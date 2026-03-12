package com.nearpick.nearpick.product.repository

import java.math.BigDecimal

interface ProductNearbyProjection {
    val id: Long
    val title: String
    val price: Int
    val productType: String
    val status: String
    val popularityScore: BigDecimal
    val distanceKm: Double
    val merchantName: String
    val shopAddress: String?
    val shopLat: BigDecimal
    val shopLng: BigDecimal
    val category: String?
    val thumbnailUrl: String?
}
