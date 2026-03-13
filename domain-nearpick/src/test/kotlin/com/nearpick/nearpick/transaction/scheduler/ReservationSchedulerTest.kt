package com.nearpick.nearpick.transaction.scheduler

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.ReservationEntity
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReservationSchedulerTest {

    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var productRepository: ProductRepository

    @InjectMocks lateinit var scheduler: ReservationScheduler

    private lateinit var consumerUser: UserEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        val merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        val merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        consumerUser = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        product = ProductEntity(
            id = 10L, merchant = merchant, title = "Item",
            price = 5000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 3,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    @Test
    fun `processNoShow - CONFIRMED 만료 예약을 NO_SHOW로 변경한다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = product,
            quantity = 1, status = ReservationStatus.CONFIRMED,
        )
        whenever(reservationRepository.findConfirmedExpiredForNoShow(any())).thenReturn(listOf(reservation))

        // when
        scheduler.processNoShow()

        // then
        assertEquals(ReservationStatus.NO_SHOW, reservation.status)
    }

    @Test
    fun `processNoShow - 대상이 없으면 아무 작업도 수행하지 않는다`() {
        // given
        whenever(reservationRepository.findConfirmedExpiredForNoShow(any())).thenReturn(emptyList())

        // when
        scheduler.processNoShow()

        // then — no exception, no side effects
    }

    @Test
    fun `processExpiredPending - PENDING 만료 예약을 CANCELLED하고 재고를 복원한다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = product,
            quantity = 2, status = ReservationStatus.PENDING,
        )
        whenever(reservationRepository.findPendingExpired(any())).thenReturn(listOf(reservation))

        // when
        scheduler.processExpiredPending()

        // then
        assertEquals(ReservationStatus.CANCELLED, reservation.status)
        verify(productRepository).incrementStock(10L, 2)
        verify(productRepository).resumeIfRestored(10L)
    }

    @Test
    fun `processExpiredPending - 대상이 없으면 아무 작업도 수행하지 않는다`() {
        // given
        whenever(reservationRepository.findPendingExpired(any())).thenReturn(emptyList())

        // when
        scheduler.processExpiredPending()

        // then — no exception, no side effects
    }
}
