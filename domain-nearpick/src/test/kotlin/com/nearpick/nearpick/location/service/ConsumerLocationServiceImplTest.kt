package com.nearpick.nearpick.location.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.dto.UpdateCurrentLocationRequest
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ConsumerLocationServiceImplTest {

    @Mock lateinit var consumerProfileRepository: ConsumerProfileRepository

    @InjectMocks lateinit var service: ConsumerLocationServiceImpl

    private val request = UpdateCurrentLocationRequest(
        lat = BigDecimal("37.5665"),
        lng = BigDecimal("126.9780"),
    )

    @Test
    fun `updateCurrentLocation - 정상적으로 위치를 갱신한다`() {
        whenever(consumerProfileRepository.updateCurrentLocation(eq(1L), any(), any(), any())).thenReturn(1)

        service.updateCurrentLocation(1L, request)

        verify(consumerProfileRepository).updateCurrentLocation(eq(1L), eq(request.lat), eq(request.lng), any())
    }

    @Test
    fun `updateCurrentLocation - 소비자 프로필이 없으면 CONSUMER_NOT_FOUND 예외`() {
        whenever(consumerProfileRepository.updateCurrentLocation(any(), any(), any(), any())).thenReturn(0)

        val ex = assertThrows<BusinessException> {
            service.updateCurrentLocation(99L, request)
        }
        assertEquals(ErrorCode.CONSUMER_NOT_FOUND, ex.errorCode)
    }
}
