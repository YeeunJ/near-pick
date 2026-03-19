package com.nearpick.nearpick.review.service

import com.nearpick.domain.review.ReviewAiService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class NoOpReviewAiServiceImpl : ReviewAiService {
    override fun checkAsync(reviewId: Long) {
        // no-op: test 환경에서 Claude API 호출 생략
    }
}
