package com.nearpick.app.config

import com.nearpick.domain.user.UserRole
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

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

    fun validateToken(token: String): Boolean = runCatching { parseClaims(token); true }.getOrElse { false }

    private fun parseClaims(token: String) = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    } catch (e: JwtException) {
        throw e
    }
}
