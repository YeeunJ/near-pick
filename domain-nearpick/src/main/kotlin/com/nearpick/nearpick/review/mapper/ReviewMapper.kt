package com.nearpick.nearpick.review.mapper

import com.nearpick.domain.review.dto.AdminReviewItem
import com.nearpick.domain.review.dto.ReviewImageItem
import com.nearpick.domain.review.dto.ReviewListItem
import com.nearpick.domain.review.dto.ReviewReplyItem
import com.nearpick.domain.review.dto.ReviewResponse
import com.nearpick.nearpick.review.entity.ReviewEntity
import com.nearpick.nearpick.review.entity.ReviewImageEntity
import com.nearpick.nearpick.review.entity.ReviewReplyEntity

object ReviewMapper {

    fun toResponse(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
        reply: ReviewReplyEntity?,
    ): ReviewResponse = ReviewResponse(
        id = review.id,
        productId = review.product.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        images = images.map { toImageItem(it) },
        reply = reply?.let { toReplyItem(it) },
        reportCount = review.reportCount,
        createdAt = review.createdAt,
    )

    fun toListItem(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
        reply: ReviewReplyEntity?,
    ): ReviewListItem = ReviewListItem(
        id = review.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        images = images.map { toImageItem(it) },
        reply = reply?.let { toReplyItem(it) },
        createdAt = review.createdAt,
    )

    fun toAdminItem(
        review: ReviewEntity,
        images: List<ReviewImageEntity>,
    ): AdminReviewItem = AdminReviewItem(
        id = review.id,
        productId = review.product.id,
        userId = review.user.id,
        rating = review.rating,
        content = review.content,
        status = review.status,
        aiChecked = review.aiChecked,
        aiResult = review.aiResult,
        blindedReason = review.blindedReason,
        blindPending = review.blindPending,
        reportCount = review.reportCount,
        images = images.map { toImageItem(it) },
        createdAt = review.createdAt,
    )

    private fun toImageItem(image: ReviewImageEntity): ReviewImageItem = ReviewImageItem(
        id = image.id,
        imageUrl = image.imageUrl,
        displayOrder = image.displayOrder,
    )

    private fun toReplyItem(reply: ReviewReplyEntity): ReviewReplyItem = ReviewReplyItem(
        id = reply.id,
        merchantId = reply.merchant.userId,
        content = reply.content,
        createdAt = reply.createdAt,
    )
}
