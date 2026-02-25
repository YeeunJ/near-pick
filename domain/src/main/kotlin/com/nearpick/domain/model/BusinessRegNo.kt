package com.nearpick.domain.model

/**
 * 한국 사업자등록번호 Value Object.
 * 형식: XXX-XX-XXXXX (10자리 숫자, 하이픈 포함)
 */
@JvmInline
value class BusinessRegNo(val value: String) {
    init {
        require(value.isNotBlank()) { "Business registration number must not be blank" }
        require(REGEX.matches(value)) { "Invalid business registration number format. Expected: XXX-XX-XXXXX" }
    }

    companion object {
        private val REGEX = Regex("^\\d{3}-\\d{2}-\\d{5}$")
    }
}
