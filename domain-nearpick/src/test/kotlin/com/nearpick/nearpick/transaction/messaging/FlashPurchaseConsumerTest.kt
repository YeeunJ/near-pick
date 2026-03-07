package com.nearpick.nearpick.transaction.messaging

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.FlashPurchaseEntity
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.lenient
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class FlashPurchaseConsumerTest {

    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var redissonClient: RedissonClient

    @Mock lateinit var idempotencyBucket: RBucket<String>
    @Mock lateinit var lock: RLock

    @InjectMocks lateinit var consumer: FlashPurchaseConsumer

    private lateinit var user: UserEntity
    private lateinit var product: ProductEntity
    private lateinit var event: FlashPurchaseRequestEvent

    @BeforeEach
    fun setUp() {
        val merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        val merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "테스트샵",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        user = UserEntity(id = 1L, email = "consumer@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        product = ProductEntity(
            id = 10L, merchant = merchant, title = "Flash Item",
            price = 5000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        event = FlashPurchaseRequestEvent(
            idempotencyKey = "1-10-20260307",
            userId = 1L,
            productId = 10L,
            quantity = 1,
            requestedAt = LocalDateTime.now(),
        )

        whenever(redissonClient.getBucket<String>(any<String>())).thenReturn(idempotencyBucket)
        lenient().`when`(redissonClient.getLock(any<String>())).thenReturn(lock)
        lenient().`when`(lock.isHeldByCurrentThread).thenReturn(true)
    }

    @Test
    fun `consume - 정상 요청 시 재고를 감소시키고 CONFIRMED 상태로 저장한다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true)
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(product)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(flashPurchaseRepository.save(any())).thenReturn(
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        )

        // when
        consumer.consume(event)

        // then
        assertEquals(4, product.stock)  // 5 - 1 = 4
        verify(flashPurchaseRepository).save(any())
    }

    @Test
    fun `consume - 중복 요청이면 idempotency 체크에서 걸러지고 저장하지 않는다`() {
        // given: 이미 처리된 이벤트 (setIfAbsent returns false)
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(false)

        // when
        consumer.consume(event)

        // then: 저장 호출 없음
        verify(flashPurchaseRepository, never()).save(any())
        verify(lock, never()).tryLock(any<Long>(), any<Long>(), any())
    }

    @Test
    fun `consume - 분산 락 획득 실패 시 idempotency 키를 삭제하고 예외를 던진다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(false)

        // when / then
        val ex = assertThrows<BusinessException> { consumer.consume(event) }
        assertEquals(ErrorCode.FLASH_PURCHASE_LOCK_FAILED, ex.errorCode)
        verify(idempotencyBucket).delete()  // 재시도 가능하도록 키 삭제
        verify(flashPurchaseRepository, never()).save(any())
    }

    @Test
    fun `consume - 재고 부족 시 OUT_OF_STOCK으로 처리하고 저장하지 않는다`() {
        // given
        val soldOutProduct = product.apply { stock = 0 }
        val quantityEvent = event.copy(quantity = 1)

        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true)
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(soldOutProduct)

        // when (BusinessException은 내부에서 catch 됨 — idempotency key 유지하여 재시도 방지)
        consumer.consume(quantityEvent)

        // then: 저장 없음, 예외 전파 없음
        verify(flashPurchaseRepository, never()).save(any())
    }

    @Test
    fun `consume - 상품이 없으면 PRODUCT_NOT_FOUND로 처리하고 저장하지 않는다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true)
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(null)

        // when
        consumer.consume(event)

        // then
        verify(flashPurchaseRepository, never()).save(any())
    }

    @Test
    fun `consume - 정상 처리 후 분산 락을 해제한다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true)
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(product)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(flashPurchaseRepository.save(any())).thenReturn(
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        )

        // when
        consumer.consume(event)

        // then
        verify(lock).unlock()
    }
}
