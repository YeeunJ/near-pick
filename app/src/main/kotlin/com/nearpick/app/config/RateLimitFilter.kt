package com.nearpick.app.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class RateLimitFilter(
    private val proxyManager: LettuceBasedProxyManager<String>,
) : OncePerRequestFilter() {

    private fun getBucketConfig(isAuthPath: Boolean): BucketConfiguration {
        val bandwidth = if (isAuthPath) {
            Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()
        } else {
            Bandwidth.builder().capacity(200).refillGreedy(200, Duration.ofMinutes(1)).build()
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
