package com.nearpick.app.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val loginBandwidth: Bandwidth,
    private val apiBandwidth: Bandwidth,
) : OncePerRequestFilter() {

    private val loginBuckets = ConcurrentHashMap<String, Bucket>()
    private val apiBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val ip = resolveIp(request)
        val isAuthPath = request.method == "POST" &&
            (request.requestURI == "/auth/login" || request.requestURI.startsWith("/auth/signup"))

        val bucket = if (isAuthPath) {
            loginBuckets.computeIfAbsent(ip) { Bucket.builder().addLimit(loginBandwidth).build() }
        } else {
            apiBuckets.computeIfAbsent(ip) { Bucket.builder().addLimit(apiBandwidth).build() }
        }

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
