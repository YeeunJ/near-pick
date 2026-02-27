package com.nearpick.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class LocationTest {

    @Test
    fun `유효한 위도와 경도로 생성된다`() {
        val location = Location(BigDecimal("37.5"), BigDecimal("127.0"))
        assertEquals(BigDecimal("37.5"), location.lat)
        assertEquals(BigDecimal("127.0"), location.lng)
    }

    @Test
    fun `위도 90 초과 시 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Location(BigDecimal("90.1"), BigDecimal("0.0")) }
    }

    @Test
    fun `위도 -90 미만 시 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Location(BigDecimal("-90.1"), BigDecimal("0.0")) }
    }

    @Test
    fun `경도 180 초과 시 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Location(BigDecimal("0.0"), BigDecimal("180.1")) }
    }

    @Test
    fun `경도 -180 미만 시 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { Location(BigDecimal("0.0"), BigDecimal("-180.1")) }
    }

    @Test
    fun `경계값 위도 90과 경도 180은 생성된다`() {
        val location = Location(BigDecimal("90.0"), BigDecimal("180.0"))
        assertEquals(BigDecimal("90.0"), location.lat)
        assertEquals(BigDecimal("180.0"), location.lng)
    }

    @Test
    fun `경계값 위도 -90과 경도 -180은 생성된다`() {
        val location = Location(BigDecimal("-90.0"), BigDecimal("-180.0"))
        assertEquals(BigDecimal("-90.0"), location.lat)
        assertEquals(BigDecimal("-180.0"), location.lng)
    }
}
