package com.nearpick.app.config

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitFilterTest {

    private fun makeFilter(
        tryConsumeResult: Boolean,
        authCapacity: Long = 10,
        authRefillMinutes: Long = 1,
    ): Pair<RateLimitFilter, MutableList<String>> {
        val capturedKeys = mutableListOf<String>()
        val mockBucket = mock<Bucket>().also {
            whenever(it.tryConsume(1L)).thenReturn(tryConsumeResult)
        }
        val provider = BucketProvider { key, _: BucketConfiguration ->
            capturedKeys += key
            mockBucket
        }
        return RateLimitFilter(provider, authCapacity, authRefillMinutes) to capturedKeys
    }

    @Test
    fun `정상 요청은 chain doFilter를 호출한다`() {
        val (filter, _) = makeFilter(tryConsumeResult = true)
        val request = MockHttpServletRequest("GET", "/api/products/nearby").apply { remoteAddr = "127.0.0.1" }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock()

        filter.doFilterInternal(request, response, chain)

        verify(chain).doFilter(request, response)
        assertEquals(200, response.status)
    }

    @Test
    fun `bucket 거부 시 429를 반환하고 chain을 호출하지 않는다`() {
        val (filter, _) = makeFilter(tryConsumeResult = false)
        val request = MockHttpServletRequest("GET", "/api/products/nearby").apply { remoteAddr = "10.0.0.1" }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock()

        filter.doFilterInternal(request, response, chain)

        assertEquals(429, response.status)
        assertTrue(response.contentAsString.contains("Too many requests"))
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `POST auth login은 auth 버킷 키를 사용한다`() {
        val (filter, keys) = makeFilter(tryConsumeResult = true)
        val request = MockHttpServletRequest("POST", "/api/auth/login").apply { remoteAddr = "10.0.0.2" }

        filter.doFilterInternal(request, MockHttpServletResponse(), mock())

        assertEquals("rate:auth:10.0.0.2", keys.first())
    }

    @Test
    fun `POST auth signup은 auth 버킷 키를 사용한다`() {
        val (filter, keys) = makeFilter(tryConsumeResult = true)
        val request = MockHttpServletRequest("POST", "/api/auth/signup/consumer").apply { remoteAddr = "10.0.0.3" }

        filter.doFilterInternal(request, MockHttpServletResponse(), mock())

        assertEquals("rate:auth:10.0.0.3", keys.first())
    }

    @Test
    fun `GET 요청은 api 버킷 키를 사용한다`() {
        val (filter, keys) = makeFilter(tryConsumeResult = true)
        val request = MockHttpServletRequest("GET", "/api/products/nearby").apply { remoteAddr = "10.0.0.4" }

        filter.doFilterInternal(request, MockHttpServletResponse(), mock())

        assertEquals("rate:api:10.0.0.4", keys.first())
    }

    @Test
    fun `X-Forwarded-For 헤더가 있으면 첫 번째 IP를 버킷 키로 사용한다`() {
        val (filter, keys) = makeFilter(tryConsumeResult = true)
        val request = MockHttpServletRequest("GET", "/api/products/nearby").apply {
            remoteAddr = "192.168.0.1"
            addHeader("X-Forwarded-For", "203.0.113.5, 192.168.0.1")
        }
        val chain: FilterChain = mock()

        filter.doFilterInternal(request, MockHttpServletResponse(), chain)

        assertEquals("rate:api:203.0.113.5", keys.first())
        verify(chain).doFilter(any(), any())
    }
}
