package com.nearpick.nearpick.location.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.dto.CreateSavedLocationRequest
import com.nearpick.domain.location.dto.UpdateSavedLocationRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.location.entity.SavedLocationEntity
import com.nearpick.nearpick.location.repository.SavedLocationRepository
import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class SavedLocationServiceImplTest {

    @Mock lateinit var savedLocationRepository: SavedLocationRepository
    @Mock lateinit var consumerProfileRepository: ConsumerProfileRepository

    @InjectMocks lateinit var service: SavedLocationServiceImpl

    private lateinit var consumer: ConsumerProfileEntity
    private lateinit var savedLocation: SavedLocationEntity

    @BeforeEach
    fun setUp() {
        val user = UserEntity(id = 1L, email = "test@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        consumer = ConsumerProfileEntity(userId = 1L, user = user, nickname = "테스터")
        savedLocation = SavedLocationEntity(
            id = 10L,
            consumer = consumer,
            label = "집",
            lat = BigDecimal("37.5665"),
            lng = BigDecimal("126.9780"),
            isDefault = false,
        )
    }

    @Test
    fun `getLocations - 저장 위치 목록을 반환한다`() {
        whenever(savedLocationRepository.findAllByConsumerUserId(1L)).thenReturn(listOf(savedLocation))

        val result = service.getLocations(1L)

        assertEquals(1, result.size)
        assertEquals("집", result[0].label)
    }

    @Test
    fun `addLocation - 정상 추가 시 저장 후 응답을 반환한다`() {
        val request = CreateSavedLocationRequest(
            label = "직장", lat = BigDecimal("37.5172"), lng = BigDecimal("127.0473"), isDefault = false,
        )
        whenever(savedLocationRepository.countByConsumerUserId(1L)).thenReturn(2L)
        whenever(consumerProfileRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(savedLocationRepository.save(any())).thenReturn(
            SavedLocationEntity(id = 11L, consumer = consumer, label = "직장",
                lat = BigDecimal("37.5172"), lng = BigDecimal("127.0473"), isDefault = false)
        )

        val result = service.addLocation(1L, request)

        assertEquals("직장", result.label)
        assertFalse(result.isDefault)
    }

    @Test
    fun `addLocation - 5개 초과 시 SAVED_LOCATION_LIMIT_EXCEEDED 예외`() {
        whenever(savedLocationRepository.countByConsumerUserId(1L)).thenReturn(5L)

        val ex = assertThrows<BusinessException> {
            service.addLocation(1L, CreateSavedLocationRequest("home", BigDecimal("37.5"), BigDecimal("127.0")))
        }
        assertEquals(ErrorCode.SAVED_LOCATION_LIMIT_EXCEEDED, ex.errorCode)
        verify(savedLocationRepository, never()).save(any())
    }

    @Test
    fun `addLocation - isDefault=true이면 기존 default를 초기화하고 저장한다`() {
        val request = CreateSavedLocationRequest(
            label = "집2", lat = BigDecimal("37.5"), lng = BigDecimal("127.0"), isDefault = true,
        )
        whenever(savedLocationRepository.countByConsumerUserId(1L)).thenReturn(1L)
        whenever(consumerProfileRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(savedLocationRepository.save(any())).thenReturn(
            SavedLocationEntity(id = 12L, consumer = consumer, label = "집2",
                lat = BigDecimal("37.5"), lng = BigDecimal("127.0"), isDefault = true)
        )

        val result = service.addLocation(1L, request)

        verify(savedLocationRepository).clearAllDefault(1L)
        assertTrue(result.isDefault)
    }

    @Test
    fun `deleteLocation - 존재하지 않는 위치 삭제 시 SAVED_LOCATION_NOT_FOUND 예외`() {
        whenever(savedLocationRepository.findByIdAndConsumerUserId(99L, 1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            service.deleteLocation(1L, 99L)
        }
        assertEquals(ErrorCode.SAVED_LOCATION_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `setDefault - 기본 위치로 설정하면 기존 default가 해제된다`() {
        whenever(savedLocationRepository.findByIdAndConsumerUserId(10L, 1L)).thenReturn(savedLocation)
        whenever(savedLocationRepository.save(any())).thenReturn(
            savedLocation.also { it.isDefault = true }
        )

        val result = service.setDefault(1L, 10L)

        verify(savedLocationRepository).clearDefaultExcept(1L, 10L)
        assertTrue(result.isDefault)
    }
}
