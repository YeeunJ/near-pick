package com.nearpick.domain.review

interface ReviewAiService {
    fun checkAsync(reviewId: Long)
}
