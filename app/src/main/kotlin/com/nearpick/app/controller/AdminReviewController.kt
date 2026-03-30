package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.review.ReviewService
import com.nearpick.domain.review.dto.AdminReviewItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin - Reviews", description = "관리자 리뷰 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
class AdminReviewController(
    private val reviewService: ReviewService,
) {

    @Operation(summary = "관리자 리뷰 검토 큐 목록 조회")
    @GetMapping
    fun getAdminQueue(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminReviewItem>> =
        ApiResponse.success(reviewService.getAdminQueue(page, size))

    @Operation(summary = "관리자 수동 블라인드 처리")
    @PatchMapping("/{reviewId}/blind")
    fun blind(@PathVariable reviewId: Long): ApiResponse<Unit> {
        reviewService.adminBlind(reviewId)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "관리자 블라인드 해제")
    @PatchMapping("/{reviewId}/unblind")
    fun unblind(@PathVariable reviewId: Long): ApiResponse<Unit> {
        reviewService.adminUnblind(reviewId)
        return ApiResponse.success(Unit)
    }
}
