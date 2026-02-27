package com.nearpick.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailTest {

    @Test
    fun `유효한 이메일로 생성된다`() {
        val email = Email("user@example.com")
        assertEquals("user@example.com", email.value)
    }

    @Test
    fun `공백 이메일은 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Email("   ") }
    }

    @Test
    fun `255자를 초과하는 이메일은 예외를 던진다`() {
        val longLocal = "a".repeat(250)
        assertThrows<IllegalArgumentException> { Email("$longLocal@example.com") }
    }

    @Test
    fun `@ 없는 형식은 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Email("invalidemail.com") }
    }

    @Test
    fun `도메인 없는 형식은 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Email("user@") }
    }

    @Test
    fun `masked는 앞 2자리만 노출하고 나머지를 마스킹한다`() {
        val email = Email("abcde@example.com")
        assertEquals("ab**@example.com", email.masked())
    }

    @Test
    fun `localPart는 @ 이전 문자열을 반환한다`() {
        val email = Email("user@example.com")
        assertEquals("user", email.localPart())
    }

    @Test
    fun `atIndex가 2 이하인 경우 masked는 원본을 반환한다`() {
        val email = Email("a@example.com")
        assertEquals("a@example.com", email.masked())
    }

    @Test
    fun `경계값 255자 이메일은 생성된다`() {
        val local = "a".repeat(243)  // 243 + "@example.com"(12) = 255
        val email = Email("$local@example.com")
        assertTrue(email.value.length == 255)
    }
}
