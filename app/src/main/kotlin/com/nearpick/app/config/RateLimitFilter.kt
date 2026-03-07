package com.nearpick.app.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class RateLimitFilter(
    private val proxyManager: LettuceBasedProxyManager<String>,
    @Value("\${rate-limit.auth.capacity:10}") private val authCapacity: Long,
    @Value("\${rate-limit.auth.refill-per-minutes:1}") private val authRefillMinutes: Long,
) : OncePerRequestFilter() {

    private fun getBucketConfig(isAuthPath: Boolean): BucketConfiguration {
        val bandwidth = if (isAuthPath) {
            Bandwidth.builder().capacity(authCapacity)
                .refillGreedy(authCapacity, Duration.ofMinutes(authRefillMinutes)).build()
        } else {
            // 500 req/sec per IP (burst-capable; realistic per-user limit for load testing)
            Bandwidth.builder().capacity(500).refillGreedy(500, Duration.ofSeconds(1)).build()
        }
        return BucketConfiguration.builder().addLimit(bandwidth).build()
    }

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val ip = resolveIp(request)
        val isAuthPath = request.method == "POST" &&
            (request.requestURI == "/api/auth/login" || request.requestURI.startsWith("/api/auth/signup"))

        val bucketKey = if (isAuthPath) "rate:auth:$ip" else "rate:api:$ip"
        val config = getBucketConfig(isAuthPath)

        val bucket = proxyManager.builder().build(bucketKey) { config }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"success":false,"message":"Too many requests. Please try again later."}""")
        }
    }

    private fun resolveIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
}
