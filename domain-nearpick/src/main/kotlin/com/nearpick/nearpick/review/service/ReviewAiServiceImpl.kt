package com.nearpick.nearpick.review.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nearpick.domain.review.ReviewAiService
import com.nearpick.domain.review.ReviewStatus
import com.nearpick.nearpick.review.repository.ReviewRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
@Profile("!test")
class ReviewAiServiceImpl(
    @Value("\${anthropic.api-key:}") private val apiKey: String,
    private val reviewRepository: ReviewRepository,
    private val objectMapper: ObjectMapper,
) : ReviewAiService {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .defaultHeader("x-api-key", apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("Content-Type", "application/json")
        .build()

    @Async("reviewAiExecutor")
    override fun checkAsync(reviewId: Long) {
        val review = reviewRepository.findById(reviewId).orElse(null) ?: return
        val product = review.product

        try {
            val result = callClaude(
                productTitle = product.title,
                category = product.category?.name ?: "기타",
                reviewContent = review.content,
                ocrText = "(없음)",
            )
            when (result.result) {
                "pass" -> {
                    review.aiChecked = true
                    review.aiResult = "pass"
                }
                "fail" -> {
                    review.aiChecked = true
                    review.aiResult = "fail"
                    review.status = ReviewStatus.BLINDED
                    review.blindedReason = "AI_DETECTED"
                }
                "need_review" -> {
                    review.aiChecked = true
                    review.aiResult = "need_review"
                    review.blindPending = true
                }
                else -> {
                    review.blindPending = true
                }
            }
            reviewRepository.save(review)
        } catch (e: Exception) {
            review.blindPending = true
            reviewRepository.save(review)
        }
    }

    private fun callClaude(
        productTitle: String,
        category: String,
        reviewContent: String,
        ocrText: String,
    ): AiReviewResult {
        val systemPrompt = """
            너는 전자상거래 플랫폼 리뷰 정책 심사관이다.
            제공된 상품명·카테고리·리뷰 텍스트·OCR 텍스트만 사용해 판단한다.
            외부 지식이나 추론은 사용하지 않는다.

            판단 기준:
            - fail       : 비속어/욕설, 특정인 비방, 개인정보 노출, 광고성 스팸, 무의미 반복 텍스트
            - pass       : 그 외 정상 리뷰
            - need_review: 경계선 사례 또는 맥락이 불분명한 경우

            반드시 아래 JSON만 출력한다. 다른 텍스트는 절대 포함하지 않는다:
            {"result":"pass"|"fail"|"need_review","reason":"fail·need_review 시 한 문장 필수, pass 시 null"}
        """.trimIndent()

        val userPrompt = """
            상품명: $productTitle
            카테고리: $category
            리뷰: $reviewContent
            OCR: $ocrText
        """.trimIndent()

        val requestBody = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 200,
            "system" to systemPrompt,
            "messages" to listOf(mapOf("role" to "user", "content" to userPrompt)),
        )

        val response = restClient.post()
            .uri("/v1/messages")
            .body(requestBody)
            .retrieve()
            .body(ClaudeApiResponse::class.java)
            ?: throw RuntimeException("Claude API returned null")

        val jsonText = response.content.first().text
        return objectMapper.readValue(jsonText, AiReviewResult::class.java)
    }
}

data class AiReviewResult(
    val result: String,
    val reason: String?,
)

data class ClaudeApiResponse(
    val content: List<ClaudeContent>,
)

data class ClaudeContent(
    val text: String,
)
