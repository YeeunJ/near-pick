package com.nearpick.app.location

import com.nearpick.domain.location.LocationClient
import com.nearpick.domain.location.dto.LocationSearchResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@Profile("local")
class StubLocationClient : LocationClient {

    override fun searchAddress(query: String): List<LocationSearchResult> = listOf(
        LocationSearchResult(
            address = "[stub] $query — 서울특별시 강남구 테헤란로 152",
            lat = BigDecimal("37.5000"),
            lng = BigDecimal("127.0367"),
        ),
        LocationSearchResult(
            address = "[stub] $query — 서울특별시 마포구 홍익로 6",
            lat = BigDecimal("37.5546"),
            lng = BigDecimal("126.9228"),
        ),
    )
}
