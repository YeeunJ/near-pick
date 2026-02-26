package com.nearpick.nearpick.product.repository

interface ProductNearbyProjection {
    val id: Long
    val title: String
    val price: Int
    val productType: String
    val status: String
    val popularityScore: Double
    val distanceKm: Double
    val merchantName: String
}
