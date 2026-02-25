package com.nearpick.domain.model

/**
 * 평문 비밀번호 Value Object.
 * 저장되지 않으며, 회원가입/변경 시 유효성 검증 용도로만 사용한다.
 */
@JvmInline
value class Password(val value: String) {
    init {
        require(value.length >= 8) { "Password must be at least 8 characters" }
        require(value.any { it.isDigit() }) { "Password must contain at least one digit" }
        require(value.any { it.isLetter() }) { "Password must contain at least one letter" }
    }
}
