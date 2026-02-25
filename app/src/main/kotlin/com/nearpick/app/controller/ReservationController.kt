package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.reservation.ReservationService
import com.nearpick.domain.reservation.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.ReservationStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/reservations")
class ReservationController(
    private val reservationService: ReservationService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReservationCreateRequest,
    ) = ApiResponse.success(reservationService.create(userId, request))

    @GetMapping("/me")
    fun getMyList(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(reservationService.getMyList(userId))

    @PatchMapping("/{id}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ) = ApiResponse.success(reservationService.cancel(userId, id))

    @GetMapping("/merchant")
    fun getMerchantList(
        @AuthenticationPrincipal merchantId: Long,
        @RequestParam(required = false) status: ReservationStatus?,
    ) = ApiResponse.success(reservationService.getMerchantList(merchantId, status))

    @PatchMapping("/{id}/confirm")
    fun confirm(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable id: Long,
    ) = ApiResponse.success(reservationService.confirm(merchantId, id))
}
