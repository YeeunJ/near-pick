package com.nearpick.domain.model

import java.math.BigDecimal

/**
 * 위치 Value Object (위도/경도 쌍).
 * lat: -90.0 ~ 90.0, lng: -180.0 ~ 180.0
 */
data class Location(val lat: BigDecimal, val lng: BigDecimal) {
    init {
        require(lat >= BigDecimal("-90.0") && lat <= BigDecimal("90.0")) {
            "Latitude must be between -90.0 and 90.0, got: $lat"
        }
        require(lng >= BigDecimal("-180.0") && lng <= BigDecimal("180.0")) {
            "Longitude must be between -180.0 and 180.0, got: $lng"
        }
    }
}
