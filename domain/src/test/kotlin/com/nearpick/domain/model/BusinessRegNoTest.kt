package com.nearpick.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BusinessRegNoTest {

    @Test
    fun `유효한 사업자등록번호로 생성된다`() {
        val regNo = BusinessRegNo("123-45-67890")
        assertEquals("123-45-67890", regNo.value)
    }

    @Test
    fun `공백은 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("   ") }
    }

    @Test
    fun `하이픈 없는 형식은 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("1234567890") }
    }

    @Test
    fun `첫 번째 그룹 자릿수 오류는 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("12-45-67890") }
    }

    @Test
    fun `두 번째 그룹 자릿수 오류는 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("123-4-67890") }
    }

    @Test
    fun `세 번째 그룹 자릿수 오류는 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("123-45-6789") }
    }

    @Test
    fun `문자 포함 시 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { BusinessRegNo("123-AB-67890") }
    }
}
