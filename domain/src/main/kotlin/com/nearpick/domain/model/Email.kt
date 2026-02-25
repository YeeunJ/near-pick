package com.nearpick.domain.model

/**
 * 이메일 주소 Value Object.
 * API 경계(DTO)의 Bean Validation과 별개로, 도메인 레이어에서 항상 유효한 이메일임을 보장한다.
 */
@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email must not be blank" }
        require(value.length <= 255) { "Email must not exceed 255 characters" }
        require(EMAIL_REGEX.matches(value)) { "Invalid email format: $value" }
    }

    fun masked(): String {
        val atIndex = value.indexOf('@')
        if (atIndex <= 2) return value
        return value.take(2) + "**" + value.substring(atIndex)
    }

    fun localPart(): String = value.substringBefore('@')

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
