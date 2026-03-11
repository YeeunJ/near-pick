package com.nearpick.domain.location

import com.nearpick.domain.location.dto.LocationSearchResult

interface LocationSearchService {
    fun search(query: String): List<LocationSearchResult>
}
