package com.nearpick.nearpick.location.service

import com.nearpick.domain.location.LocationClient
import com.nearpick.domain.location.LocationSearchService
import com.nearpick.domain.location.dto.LocationSearchResult
import org.springframework.stereotype.Service

@Service
class LocationSearchServiceImpl(
    private val locationClient: LocationClient,
) : LocationSearchService {

    override fun search(query: String): List<LocationSearchResult> =
        locationClient.searchAddress(query)
}
