package com.nearpick.app.location

import com.nearpick.domain.location.LocationClient
import com.nearpick.domain.location.dto.LocationSearchResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpLocationClient : LocationClient {
    override fun searchAddress(query: String): List<LocationSearchResult> = emptyList()
}
