package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.transaction.ReservationService
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.transaction.dto.ReservationVisitRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Reservations", description = "예약 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/reservations")
class ReservationController(private val reservationService: ReservationService) {

    @Operation(summary = "예약 생성 (소비자 전용)")
    @SwaggerApiResponse(responseCode = "201", description = "예약 성공")
    @SwaggerApiResponse(responseCode = "400", description = "비활성 상품")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONSUMER')")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReservationCreateRequest,
    ) = ApiResponse.success(reservationService.create(userId, request.productId, request))

    @Operation(summary = "예약 취소 (소비자 전용)")
    @SwaggerApiResponse(responseCode = "200", description = "취소 성공")
    @SwaggerApiResponse(responseCode = "403", description = "권한 없음")
    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('CONSUMER')")
    fun cancel(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.cancel(userId, reservationId))

    @Operation(summary = "예약 확정 (소상공인 전용)")
    @SwaggerApiResponse(responseCode = "200", description = "확정 성공")
    @PatchMapping("/{reservationId}/confirm")
    @PreAuthorize("hasRole('MERCHANT')")
    fun confirm(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.confirm(userId, reservationId))

    @Operation(summary = "내 예약 목록 조회 (소비자 전용)")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getMyReservations(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(reservationService.getMyReservations(userId, page, size))

    @Operation(summary = "대기 예약 목록 조회 (소상공인 전용, deprecated)", description = "getMerchantReservations로 대체됨")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/merchant/pending")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getPendingReservations(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(reservationService.getPendingReservations(userId, page, size))

    @Operation(summary = "예약 상세 조회 (소비자/소상공인)", description = "소비자 본인은 visitCode 포함")
    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
    fun getDetail(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.getDetail(userId, reservationId))

    @Operation(summary = "방문 코드 확인 → 완료 처리 (소상공인 전용)")
    @PatchMapping("/visit")
    @PreAuthorize("hasRole('MERCHANT')")
    fun visit(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReservationVisitRequest,
    ) = ApiResponse.success(reservationService.visitByCode(userId, request))

    @Operation(summary = "소상공인 예약 취소 (PENDING/CONFIRMED → CANCELLED)")
    @PatchMapping("/{reservationId}/cancel-by-merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    fun cancelByMerchant(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reservationId: Long,
    ) = ApiResponse.success(reservationService.cancelByMerchant(userId, reservationId))

    @Operation(summary = "소상공인 예약 목록 조회 (상태 필터)")
    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    fun getMerchantReservations(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(required = false) status: ReservationStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(reservationService.getMerchantReservations(userId, status, page, size))
}
