package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.location.ConsumerLocationService
import com.nearpick.domain.location.SavedLocationService
import com.nearpick.domain.location.dto.CreateSavedLocationRequest
import com.nearpick.domain.location.dto.SavedLocationResponse
import com.nearpick.domain.location.dto.UpdateCurrentLocationRequest
import com.nearpick.domain.location.dto.UpdateSavedLocationRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Consumer Location", description = "소비자 위치 관련 API")
@RestController
@RequestMapping("/api/consumers/me")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('CONSUMER')")
class ConsumerLocationController(
    private val consumerLocationService: ConsumerLocationService,
    private val savedLocationService: SavedLocationService,
) {

    @Operation(summary = "현재 위치 갱신", description = "GPS 위치를 서버에 저장한다.")
    @PatchMapping("/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateCurrentLocation(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdateCurrentLocationRequest,
    ) {
        consumerLocationService.updateCurrentLocation(userId, request)
    }

    @Operation(summary = "저장 위치 목록 조회")
    @GetMapping("/locations")
    fun getLocations(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<SavedLocationResponse>> =
        ApiResponse.success(savedLocationService.getLocations(userId))

    @Operation(summary = "저장 위치 추가", description = "최대 5개까지 저장 가능")
    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    fun addLocation(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateSavedLocationRequest,
    ): ApiResponse<SavedLocationResponse> =
        ApiResponse.success(savedLocationService.addLocation(userId, request))

    @Operation(summary = "저장 위치 수정", description = "label, isDefault 변경 가능")
    @PutMapping("/locations/{id}")
    fun updateLocation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSavedLocationRequest,
    ): ApiResponse<SavedLocationResponse> =
        ApiResponse.success(savedLocationService.updateLocation(userId, id, request))

    @Operation(summary = "저장 위치 삭제")
    @DeleteMapping("/locations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteLocation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ) {
        savedLocationService.deleteLocation(userId, id)
    }

    @Operation(summary = "기본 위치 지정", description = "해당 위치를 기본 위치로 설정한다.")
    @PatchMapping("/locations/{id}/default")
    fun setDefault(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<SavedLocationResponse> =
        ApiResponse.success(savedLocationService.setDefault(userId, id))
}
