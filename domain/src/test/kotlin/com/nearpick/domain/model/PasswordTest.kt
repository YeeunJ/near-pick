package com.nearpick.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PasswordTest {

    @Test
    fun `유효한 비밀번호로 생성된다`() {
        val password = Password("pass1234")
        kotlin.test.assertEquals("pass1234", password.value)
    }

    @Test
    fun `8자 미만이면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Password("pass123") }
    }

    @Test
    fun `숫자가 없으면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Password("password") }
    }

    @Test
    fun `문자가 없으면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Password("12345678") }
    }

    @Test
    fun `정확히 8자인 경우 생성된다`() {
        val password = Password("abcd1234")
        kotlin.test.assertEquals("abcd1234", password.value)
    }
}
