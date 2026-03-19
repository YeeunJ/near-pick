package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.review.ReviewImageService
import com.nearpick.domain.review.ReviewReplyService
import com.nearpick.domain.review.ReviewService
import com.nearpick.domain.review.dto.ReviewCreateRequest
import com.nearpick.domain.review.dto.ReviewImageUploadResponse
import com.nearpick.domain.review.dto.ReviewListItem
import com.nearpick.domain.review.dto.ReviewReplyCreateRequest
import com.nearpick.domain.review.dto.ReviewResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Reviews", description = "리뷰 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val reviewReplyService: ReviewReplyService,
    private val reviewImageService: ReviewImageService,
) {

    @Operation(summary = "리뷰 작성 (소비자 전용)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONSUMER')")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReviewCreateRequest,
    ) = ApiResponse.success(reviewService.create(userId, request))

    @Operation(summary = "상품 리뷰 목록 조회 (공개)")
    @GetMapping("/product/{productId}")
    fun getProductReviews(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<ReviewListItem>> =
        ApiResponse.success(reviewService.getProductReviews(productId, page, size))

    @Operation(summary = "내 리뷰 목록 조회 (소비자 전용)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getMyReviews(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<ReviewListItem>> =
        ApiResponse.success(reviewService.getMyReviews(userId, page, size))

    @Operation(summary = "리뷰 삭제 (소비자 전용, soft delete)")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CONSUMER')")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        reviewService.delete(userId, reviewId)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "리뷰 이미지 Presigned URL 발급 (소비자 전용)")
    @PostMapping("/{reviewId}/images")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getPresignedUrl(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
        @RequestParam contentType: String,
    ): ApiResponse<ReviewImageUploadResponse> =
        ApiResponse.success(reviewImageService.getPresignedUrl(userId, reviewId, contentType))

    @Operation(summary = "리뷰 이미지 업로드 완료 확인 (소비자 전용)")
    @PostMapping("/{reviewId}/images/confirm")
    @PreAuthorize("hasRole('CONSUMER')")
    fun confirmImageUpload(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
        @RequestParam s3Key: String,
        @RequestParam imageUrl: String,
    ): ApiResponse<Unit> {
        reviewImageService.confirmUpload(userId, reviewId, s3Key, imageUrl)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "소상공인 답글 작성 (소상공인 전용)")
    @PostMapping("/{reviewId}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MERCHANT')")
    fun createReply(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable reviewId: Long,
        @RequestBody @Valid request: ReviewReplyCreateRequest,
    ) = ApiResponse.success(reviewReplyService.create(merchantId, reviewId, request))

    @Operation(summary = "소상공인 답글 삭제 (소상공인 전용)")
    @DeleteMapping("/{reviewId}/reply")
    @PreAuthorize("hasRole('MERCHANT')")
    fun deleteReply(
        @AuthenticationPrincipal merchantId: Long,
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        reviewReplyService.delete(merchantId, reviewId)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "리뷰 신고 (소비자 전용)")
    @PostMapping("/{reviewId}/report")
    @PreAuthorize("hasRole('CONSUMER')")
    fun report(
        @AuthenticationPrincipal userId: Long,
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        reviewService.report(userId, reviewId)
        return ApiResponse.success(Unit)
    }
}
