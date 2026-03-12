package com.nearpick.domain.location

import com.nearpick.domain.location.dto.LocationSearchResult

interface LocationClient {
    fun searchAddress(query: String): List<LocationSearchResult>
}
