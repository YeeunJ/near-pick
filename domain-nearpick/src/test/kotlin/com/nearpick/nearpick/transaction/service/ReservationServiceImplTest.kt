package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.transaction.dto.ReservationCreateRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.ReservationEntity
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReservationServiceImplTest {

    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository

    @InjectMocks lateinit var reservationService: ReservationServiceImpl

    private lateinit var consumerUser: UserEntity
    private lateinit var merchantUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var activeProduct: ProductEntity

    @BeforeEach
    fun setUp() {
        consumerUser = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        activeProduct = ProductEntity(
            id = 10L, merchant = merchant, title = "Reservation Item",
            price = 5000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    // ───── create ─────

    @Test
    fun `create - 정상 요청 시 ReservationStatusResponse를 반환한다`() {
        // given
        val request = ReservationCreateRequest(productId = 10L, quantity = 1, memo = null, visitScheduledAt = null)
        val savedReservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct, quantity = 1,
        )
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumerUser))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct))
        whenever(reservationRepository.save(any())).thenReturn(savedReservation)

        // when
        val response = reservationService.create(1L, 10L, request)

        // then
        assertEquals(ReservationStatus.PENDING, response.status)
    }

    @Test
    fun `create - 비활성 상품이면 PRODUCT_NOT_ACTIVE 예외를 던진다`() {
        // given
        val request = ReservationCreateRequest(productId = 10L, quantity = 1, memo = null, visitScheduledAt = null)
        val closedProduct = activeProduct.apply { status = ProductStatus.CLOSED }
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumerUser))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(closedProduct))

        // when / then
        val ex = assertThrows<BusinessException> { reservationService.create(1L, 10L, request) }
        assertEquals(ErrorCode.PRODUCT_NOT_ACTIVE, ex.errorCode)
    }

    // ───── cancel ─────

    @Test
    fun `cancel - 본인 예약이고 PENDING 상태이면 CANCELLED로 변경된다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.PENDING,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when
        val response = reservationService.cancel(1L, 1L)

        // then
        assertEquals(ReservationStatus.CANCELLED, response.status)
    }

    @Test
    fun `cancel - 다른 사용자의 예약이면 FORBIDDEN 예외를 던진다`() {
        // given
        val otherUser = UserEntity(id = 99L, email = "other@example.com", passwordHash = "h", role = UserRole.CONSUMER)
        val reservation = ReservationEntity(
            id = 1L, user = otherUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.PENDING,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when / then
        val ex = assertThrows<BusinessException> { reservationService.cancel(1L, 1L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `cancel - PENDING이 아닌 예약은 RESERVATION_CANNOT_BE_CANCELLED 예외를 던진다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.CONFIRMED,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when / then
        val ex = assertThrows<BusinessException> { reservationService.cancel(1L, 1L) }
        assertEquals(ErrorCode.RESERVATION_CANNOT_BE_CANCELLED, ex.errorCode)
    }

    // ───── confirm ─────

    @Test
    fun `confirm - 해당 소상공인의 예약이고 PENDING 상태이면 CONFIRMED로 변경된다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.PENDING,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when
        val response = reservationService.confirm(2L, 1L)  // merchantId = 2L

        // then
        assertEquals(ReservationStatus.CONFIRMED, response.status)
    }

    @Test
    fun `confirm - 다른 소상공인이 확정 시도하면 FORBIDDEN 예외를 던진다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.PENDING,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when / then
        val ex = assertThrows<BusinessException> { reservationService.confirm(99L, 1L) }  // 다른 merchantId
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `confirm - PENDING이 아닌 예약은 RESERVATION_CANNOT_BE_CONFIRMED 예외를 던진다`() {
        // given
        val reservation = ReservationEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = ReservationStatus.CANCELLED,
        )
        whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

        // when / then
        val ex = assertThrows<BusinessException> { reservationService.confirm(2L, 1L) }
        assertEquals(ErrorCode.RESERVATION_CANNOT_BE_CONFIRMED, ex.errorCode)
    }
}
