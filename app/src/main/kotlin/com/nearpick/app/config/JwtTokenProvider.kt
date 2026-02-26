package com.nearpick.app.config

import com.nearpick.domain.user.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        val keyBytes = secret.toByteArray()
        check(keyBytes.size >= 32) { "jwt.secret must be at least 32 characters (got ${keyBytes.size})" }
        key = Keys.hmacShaKeyFor(keyBytes)
    }

    fun createToken(userId: Long, role: UserRole): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .issuedAt(java.util.Date())
            .expiration(java.util.Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun getUserId(token: String): Long = parseClaims(token).subject.toLong()

    fun getRole(token: String): UserRole =
        UserRole.valueOf(parseClaims(token).get("role", String::class.java))

    fun validateToken(token: String): Boolean =
        runCatching { parseClaims(token); true }.getOrElse { false }

    private fun parseClaims(token: String) =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
