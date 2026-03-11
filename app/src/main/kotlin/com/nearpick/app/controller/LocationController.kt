package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.location.LocationSearchService
import com.nearpick.domain.location.dto.LocationSearchResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Location", description = "위치 검색 API")
@RestController
@RequestMapping("/api/location")
@SecurityRequirement(name = "Bearer Authentication")
class LocationController(
    private val locationSearchService: LocationSearchService,
) {

    @Operation(summary = "주소 검색", description = "카카오 주소 검색 API를 통해 텍스트를 좌표로 변환한다. 최대 5건 반환.")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
    fun search(
        @RequestParam query: String,
    ): ApiResponse<List<LocationSearchResult>> =
        ApiResponse.success(locationSearchService.search(query))
}
