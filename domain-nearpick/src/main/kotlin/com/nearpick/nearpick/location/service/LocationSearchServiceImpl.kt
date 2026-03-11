package com.nearpick.nearpick.location.service

import com.nearpick.domain.location.LocationSearchService
import com.nearpick.domain.location.dto.LocationSearchResult
import com.nearpick.nearpick.location.client.KakaoLocationClient
import org.springframework.stereotype.Service

@Service
class LocationSearchServiceImpl(
    private val kakaoLocationClient: KakaoLocationClient,
) : LocationSearchService {

    override fun search(query: String): List<LocationSearchResult> =
        kakaoLocationClient.searchAddress(query)
}
