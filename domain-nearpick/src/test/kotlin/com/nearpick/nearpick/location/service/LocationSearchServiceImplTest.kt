package com.nearpick.nearpick.location.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.dto.LocationSearchResult
import com.nearpick.nearpick.location.client.KakaoLocationClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class LocationSearchServiceImplTest {

    @Mock lateinit var kakaoLocationClient: KakaoLocationClient

    @InjectMocks lateinit var service: LocationSearchServiceImpl

    @Test
    fun `search - 카카오 결과를 그대로 반환한다`() {
        val results = listOf(
            LocationSearchResult("서울 강남구 테헤란로 1", BigDecimal("37.4979"), BigDecimal("127.0276"))
        )
        whenever(kakaoLocationClient.searchAddress("강남")).thenReturn(results)

        val actual = service.search("강남")

        assertEquals(1, actual.size)
        assertEquals("서울 강남구 테헤란로 1", actual[0].address)
    }

    @Test
    fun `search - 결과 없으면 빈 리스트 반환`() {
        whenever(kakaoLocationClient.searchAddress("없는주소xyz")).thenReturn(emptyList())

        val actual = service.search("없는주소xyz")

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `search - 카카오 API 실패 시 EXTERNAL_API_UNAVAILABLE 예외 전파`() {
        whenever(kakaoLocationClient.searchAddress("강남"))
            .thenThrow(BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE))

        val ex = assertThrows<BusinessException> {
            service.search("강남")
        }
        assertEquals(ErrorCode.EXTERNAL_API_UNAVAILABLE, ex.errorCode)
    }
}
