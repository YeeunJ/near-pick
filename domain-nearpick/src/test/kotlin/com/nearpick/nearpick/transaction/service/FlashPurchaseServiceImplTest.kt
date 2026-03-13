package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.domain.transaction.dto.FlashPurchasePickupRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseEventProducer
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseRequestEvent
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RAtomicLong
import org.redisson.api.RedissonClient
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * FlashPurchaseServiceImpl 단위 테스트
 *
 * Phase 9에서 서비스가 Kafka 비동기 방식으로 전환됨:
 * - 재고 감소/구매자 조회는 Consumer로 이동
 * - 서비스는 Kafka 이벤트 발행 + 즉시 PENDING 반환만 수행
 * - 재고/구매자 검증 로직은 FlashPurchaseConsumerTest에서 검증
 */
@ExtendWith(MockitoExtension::class)
class FlashPurchaseServiceImplTest {

    @Mock lateinit var producer: FlashPurchaseEventProducer
    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var redissonClient: RedissonClient

    @InjectMocks lateinit var service: FlashPurchaseServiceImpl

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
            id = 10L, merchant = merchant, title = "Flash Item",
            price = 3000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    @Test
    fun `purchase - Kafka 이벤트를 발행하고 즉시 PENDING 상태를 반환한다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 2)

        // when
        val response = service.purchase(userId = 1L, productId = 10L, request = request)

        // then
        assertEquals(FlashPurchaseStatus.PENDING, response.status)
        assertNull(response.purchaseId)
        verify(producer).send(any<FlashPurchaseRequestEvent>())
    }

    @Test
    fun `purchase - 멱등성 키는 userId-productId-날짜 형식으로 생성된다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 1)
        val eventCaptor = argumentCaptor<FlashPurchaseRequestEvent>()

        // when
        service.purchase(userId = 5L, productId = 10L, request = request)

        // then
        verify(producer).send(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(5L, event.userId)
        assertEquals(10L, event.productId)
        assertEquals(1, event.quantity)
        // idempotencyKey 형식: "5-10-YYYYMMDD"
        assert(event.idempotencyKey.startsWith("5-10-")) {
            "idempotencyKey should start with '5-10-', got: ${event.idempotencyKey}"
        }
    }

    @Test
    fun `purchase - 서로 다른 사용자의 동일 상품 구매 이벤트는 다른 멱등성 키를 가진다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 1)
        val captor = argumentCaptor<FlashPurchaseRequestEvent>()

        // when
        service.purchase(userId = 1L, productId = 10L, request = request)
        service.purchase(userId = 2L, productId = 10L, request = request)

        // then
        verify(producer, org.mockito.kotlin.times(2)).send(captor.capture())
        val (event1, event2) = captor.allValues
        assert(event1.idempotencyKey != event2.idempotencyKey) {
            "Different users must have different idempotency keys"
        }
    }

    // ───── cancelByMerchant (Phase 12) ────────────────────────────────

    @Test
    fun `cancelByMerchant - CONFIRMED 구매를 취소하고 DB와 Redis 재고를 복원한다`() {
        // given
        val purchase = FlashPurchaseEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 2, status = FlashPurchaseStatus.CONFIRMED, pickupCode = "XYZ123",
        )
        val atomicLong = org.mockito.kotlin.mock<RAtomicLong>()
        whenever(flashPurchaseRepository.findById(1L)).thenReturn(Optional.of(purchase))
        whenever(flashPurchaseRepository.save(any())).thenReturn(purchase)
        whenever(redissonClient.getAtomicLong("stock:flash:10")).thenReturn(atomicLong)

        // when
        val response = service.cancelByMerchant(merchantId = 2L, purchaseId = 1L)

        // then
        assertEquals(FlashPurchaseStatus.CANCELLED, response.status)
        verify(productRepository).incrementStock(10L, 2)
        verify(productRepository).resumeIfRestored(10L)
        verify(atomicLong).addAndGet(2L)
    }

    @Test
    fun `cancelByMerchant - CONFIRMED가 아닌 구매는 FLASH_PURCHASE_CANNOT_BE_CANCELLED 예외를 던진다`() {
        // given
        val purchase = FlashPurchaseEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = FlashPurchaseStatus.PICKED_UP,
        )
        whenever(flashPurchaseRepository.findById(1L)).thenReturn(Optional.of(purchase))

        // when / then
        val ex = assertThrows<BusinessException> { service.cancelByMerchant(merchantId = 2L, purchaseId = 1L) }
        assertEquals(ErrorCode.FLASH_PURCHASE_CANNOT_BE_CANCELLED, ex.errorCode)
    }

    // ───── pickupByCode (Phase 12) ────────────────────────────────────

    @Test
    fun `pickupByCode - 유효한 픽업 코드로 CONFIRMED 구매를 PICKED_UP으로 처리한다`() {
        // given
        val purchase = FlashPurchaseEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = FlashPurchaseStatus.CONFIRMED, pickupCode = "ABC123",
        )
        whenever(flashPurchaseRepository.findByPickupCode("ABC123")).thenReturn(purchase)
        whenever(flashPurchaseRepository.save(any())).thenReturn(purchase)

        // when
        val response = service.pickupByCode(merchantId = 2L, request = FlashPurchasePickupRequest("ABC123"))

        // then
        assertEquals(FlashPurchaseStatus.PICKED_UP, response.status)
        assertNull(purchase.pickupCode)
        assertNotNull(purchase.pickedUpAt)
    }

    @Test
    fun `pickupByCode - 유효하지 않은 코드이면 FLASH_PURCHASE_PICKUP_CODE_INVALID 예외를 던진다`() {
        whenever(flashPurchaseRepository.findByPickupCode("XXXXXX")).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            service.pickupByCode(merchantId = 2L, request = FlashPurchasePickupRequest("XXXXXX"))
        }
        assertEquals(ErrorCode.FLASH_PURCHASE_PICKUP_CODE_INVALID, ex.errorCode)
    }

    // ───── getMerchantPurchases (Phase 12) ────────────────────────────

    @Test
    fun `getMerchantPurchases - 상태 필터 없이 소상공인의 전체 구매 목록을 반환한다`() {
        // given
        val purchase = FlashPurchaseEntity(
            id = 1L, user = consumerUser, product = activeProduct,
            quantity = 1, status = FlashPurchaseStatus.CONFIRMED,
        )
        val pageable = PageRequest.of(0, 20)
        whenever(flashPurchaseRepository.findByMerchantIdAndOptionalStatus(2L, null, pageable))
            .thenReturn(PageImpl(listOf(purchase)))

        // when
        val result = service.getMerchantPurchases(merchantId = 2L, status = null, page = 0, size = 20)

        // then
        assertEquals(1, result.totalElements)
        assertEquals(FlashPurchaseStatus.CONFIRMED, result.content.first().status)
    }
}
