package com.nearpick.domain.product.dto

import java.math.BigDecimal

data class ProductNearbyRequest(
    val lat: BigDecimal,
    val lng: BigDecimal,
    val radius: Double = 5.0,
    val sort: String = "popularity",
    val page: Int = 0,
    val size: Int = 20,
)
