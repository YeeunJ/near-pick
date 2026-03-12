package com.nearpick.nearpick.transaction.service

import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseEventProducer
import com.nearpick.nearpick.transaction.messaging.FlashPurchaseRequestEvent
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
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

    @InjectMocks lateinit var service: FlashPurchaseServiceImpl

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
}
