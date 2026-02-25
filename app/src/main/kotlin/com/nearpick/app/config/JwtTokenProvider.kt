package com.nearpick.app.config

import com.nearpick.domain.user.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {
    private lateinit var key: javax.crypto.SecretKey

    @PostConstruct
    fun init() {
        val keyBytes = secret.toByteArray()
        if (keyBytes.size < 32) {
            throw IllegalStateException(
                "jwt.secret must be at least 32 characters. Current length: ${keyBytes.size}"
            )
        }
        try {
            key = Keys.hmacShaKeyFor(keyBytes)
        } catch (e: WeakKeyException) {
            throw IllegalStateException("JWT secret key is too weak: ${e.message}", e)
        }
    }

    fun createToken(userId: Long, role: UserRole): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun getUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    fun getRole(token: String): UserRole =
        UserRole.valueOf(parseClaims(token).get("role", String::class.java))

    fun validateToken(token: String): Boolean =
        runCatching { parseClaims(token); true }.getOrElse { false }

    private fun parseClaims(token: String) =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
