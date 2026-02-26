package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.ReservationService
import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reservations")
class ReservationController(private val reservationService: ReservationService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONSUMER')")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ReservationCreateRequest,
    ) = ApiResponse.success(reservationService.create(userId, request.productId, request))

    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('CONSUMER')")
    fun cancel(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.cancel(userId, reservationId))

    @PatchMapping("/{reservationId}/confirm")
    @PreAuthorize("hasRole('MERCHANT')")
    fun confirm(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.confirm(userId, reservationId))

    @GetMapping("/me")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getMyReservations(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(reservationService.getMyReservations(userId, page, size))

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getPendingReservations(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(reservationService.getPendingReservations(userId, page, size))
}
