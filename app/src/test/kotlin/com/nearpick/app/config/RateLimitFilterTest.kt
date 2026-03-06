package com.nearpick.app.config

import io.github.bucket4j.Bandwidth
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitFilterTest {

    private lateinit var loginBandwidth: Bandwidth
    private lateinit var apiBandwidth: Bandwidth
    private lateinit var filter: RateLimitFilter

    @BeforeEach
    fun setUp() {
        // 각 테스트에서 새 Bandwidth(버킷)로 초기화 — 독립된 상태
        loginBandwidth = Bandwidth.builder().capacity(2).refillGreedy(2, Duration.ofMinutes(1)).build()
        apiBandwidth = Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofMinutes(1)).build()
        filter = RateLimitFilter(loginBandwidth, apiBandwidth)
    }

    @Test
    fun `정상 요청은 chain doFilter를 호출한다`() {
        val request = MockHttpServletRequest("GET", "/api/products/nearby")
        request.remoteAddr = "127.0.0.1"
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock()

        filter.doFilterInternal(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `apiBucket 용량 초과 시 429를 반환한다`() {
        val chain: FilterChain = mock()

        // apiBandwidth capacity=3 → 4번째 요청에서 429
        repeat(3) {
            val req = MockHttpServletRequest("GET", "/api/products/nearby").apply { remoteAddr = "10.0.0.1" }
            filter.doFilterInternal(req, MockHttpServletResponse(), chain)
        }

        val req = MockHttpServletRequest("GET", "/api/products/nearby").apply { remoteAddr = "10.0.0.1" }
        val response = MockHttpServletResponse()
        filter.doFilterInternal(req, response, chain)

        assertEquals(429, response.status)
        assertTrue(response.contentAsString.contains("Too many requests"))
        verify(chain, never()).doFilter(req, response)
    }

    @Test
    fun `POST api auth login 은 loginBucket을 사용한다`() {
        val chain: FilterChain = mock()

        // loginBandwidth capacity=2 → 3번째 요청에서 429
        repeat(2) {
            val req = MockHttpServletRequest("POST", "/api/auth/login").apply { remoteAddr = "10.0.0.2" }
            filter.doFilterInternal(req, MockHttpServletResponse(), chain)
        }

        val req = MockHttpServletRequest("POST", "/api/auth/login").apply { remoteAddr = "10.0.0.2" }
        val response = MockHttpServletResponse()
        filter.doFilterInternal(req, response, chain)

        assertEquals(429, response.status)
    }

    @Test
    fun `POST api auth signup 은 loginBucket을 사용한다`() {
        val chain: FilterChain = mock()

        repeat(2) {
            val req = MockHttpServletRequest("POST", "/api/auth/signup/consumer").apply { remoteAddr = "10.0.0.3" }
            filter.doFilterInternal(req, MockHttpServletResponse(), chain)
        }

        val req = MockHttpServletRequest("POST", "/api/auth/signup/consumer").apply { remoteAddr = "10.0.0.3" }
        val response = MockHttpServletResponse()
        filter.doFilterInternal(req, response, chain)

        assertEquals(429, response.status)
    }

    @Test
    fun `X-Forwarded-For 헤더가 있으면 해당 IP를 사용한다`() {
        val chain: FilterChain = mock()

        // remoteAddr는 프록시 IP, 실제 클라이언트 IP는 X-Forwarded-For
        val req = MockHttpServletRequest("GET", "/api/products/nearby").apply {
            remoteAddr = "192.168.0.1"
            addHeader("X-Forwarded-For", "203.0.113.5, 192.168.0.1")
        }
        val response = MockHttpServletResponse()
        filter.doFilterInternal(req, response, chain)

        // 정상 통과 (앞 IP 203.0.113.5 기준으로 카운트)
        verify(chain).doFilter(req, response)
        assertEquals(200, response.status)
    }
}
