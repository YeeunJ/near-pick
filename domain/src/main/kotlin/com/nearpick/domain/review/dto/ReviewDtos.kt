package com.nearpick.domain.review.dto

import com.nearpick.domain.review.ReviewStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ── Requests ──────────────────────────────────────────────────────────────────

data class ReviewCreateRequest(
    @field:NotNull val productId: Long,
    val reservationId: Long? = null,
    val flashPurchaseId: Long? = null,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(max = 500) val content: String,
)

data class ReviewReplyCreateRequest(
    @field:NotBlank @field:Size(max = 300) val content: String,
)

// ── Responses ─────────────────────────────────────────────────────────────────

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val images: List<ReviewImageItem>,
    val reply: ReviewReplyItem?,
    val reportCount: Int,
    val createdAt: LocalDateTime,
)

data class ReviewListItem(
    val id: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val images: List<ReviewImageItem>,
    val reply: ReviewReplyItem?,
    val createdAt: LocalDateTime,
)

data class ReviewImageItem(
    val id: Long,
    val imageUrl: String,
    val displayOrder: Int,
)

data class ReviewReplyItem(
    val id: Long,
    val merchantId: Long,
    val content: String,
    val createdAt: LocalDateTime,
)

data class ReviewImageUploadResponse(
    val presignedUrl: String,
    val imageUrl: String,
    val s3Key: String,
)

data class AdminReviewItem(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val rating: Int,
    val content: String,
    val status: ReviewStatus,
    val aiChecked: Boolean,
    val aiResult: String?,
    val blindedReason: String?,
    val blindPending: Boolean,
    val reportCount: Int,
    val images: List<ReviewImageItem>,
    val createdAt: LocalDateTime,
)
