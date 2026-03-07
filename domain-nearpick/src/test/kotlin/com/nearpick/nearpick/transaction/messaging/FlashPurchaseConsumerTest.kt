package com.nearpick.nearpick.transaction.messaging

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
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.lenient
import org.redisson.api.RAtomicLong
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class FlashPurchaseConsumerTest {

    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var redissonClient: RedissonClient
    @Mock lateinit var meterRegistry: MeterRegistry

    @Mock lateinit var idempotencyBucket: RBucket<String>
    @Mock lateinit var stockCounter: RAtomicLong
    @Mock lateinit var initLock: RLock
    @Mock lateinit var counter: Counter

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
        lenient().`when`(redissonClient.getAtomicLong(any<String>())).thenReturn(stockCounter)
        lenient().`when`(redissonClient.getLock(any<String>())).thenReturn(initLock)
        lenient().`when`(initLock.isHeldByCurrentThread).thenReturn(true)
        lenient().`when`(stockCounter.isExists).thenReturn(true)  // 기본: 카운터 이미 초기화됨
        lenient().`when`(meterRegistry.counter(any<String>(), any<String>(), any<String>())).thenReturn(counter)
        lenient().`when`(counter.increment()).then { }
    }

    @Test
    fun `consume - 정상 요청 시 재고를 감소시키고 CONFIRMED 상태로 저장한다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(stockCounter.addAndGet(-1L)).thenReturn(4L)  // 5 - 1 = 4
        whenever(productRepository.decrementStockIfSufficient(10L, 1)).thenReturn(1)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(flashPurchaseRepository.save(any())).thenReturn(
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        )

        // when
        consumer.consume(event)

        // then
        verify(flashPurchaseRepository).save(any())
        verify(stockCounter).addAndGet(-1L)
    }

    @Test
    fun `consume - 중복 요청이면 idempotency 체크에서 걸러지고 저장하지 않는다`() {
        // given: 이미 처리된 이벤트 (setIfAbsent returns false)
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(false)

        // when
        consumer.consume(event)

        // then: 저장 호출 없음, 재고 차감 없음
        verify(flashPurchaseRepository, never()).save(any())
        verify(stockCounter, never()).addAndGet(any())
    }

    @Test
    fun `consume - 재고 부족 시 OUT_OF_STOCK으로 처리하고 저장하지 않는다`() {
        // given: Redis 카운터 차감 후 음수 반환
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(stockCounter.addAndGet(-1L)).thenReturn(-1L)  // 재고 부족

        // when
        consumer.consume(event)

        // then: 카운터 복원, 저장 없음
        verify(stockCounter).addAndGet(1L)  // 복원
        verify(flashPurchaseRepository, never()).save(any())
    }

    @Test
    fun `consume - DB와 Redis 재고 불일치 시 Redis 카운터를 복원하고 저장하지 않는다`() {
        // given: Redis 차감 성공 but DB update 실패 (재고 불일치)
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(stockCounter.addAndGet(-1L)).thenReturn(4L)  // Redis 차감 성공
        whenever(productRepository.decrementStockIfSufficient(10L, 1)).thenReturn(0)  // DB 업데이트 실패

        // when
        consumer.consume(event)

        // then: Redis 복원, 저장 없음
        verify(stockCounter).addAndGet(1L)  // Redis 복원
        verify(flashPurchaseRepository, never()).save(any())
    }

    @Test
    fun `consume - 카운터 미초기화 시 DB에서 지연 초기화 후 정상 처리한다`() {
        // given: 카운터 미초기화 (최초 요청)
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(stockCounter.isExists).thenReturn(false)
        whenever(initLock.tryLock(1L, 5L, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true)
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(stockCounter.addAndGet(-1L)).thenReturn(4L)
        whenever(productRepository.decrementStockIfSufficient(10L, 1)).thenReturn(1)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(flashPurchaseRepository.save(any())).thenReturn(
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        )

        // when
        consumer.consume(event)

        // then: 초기화 + 정상 저장
        verify(stockCounter).set(5L)   // product.stock = 5
        verify(flashPurchaseRepository).save(any())
    }

    @Test
    fun `consume - 정상 처리 후 success 메트릭을 기록한다`() {
        // given
        whenever(idempotencyBucket.setIfAbsent(any<String>(), any())).thenReturn(true)
        whenever(stockCounter.addAndGet(-1L)).thenReturn(4L)
        whenever(productRepository.decrementStockIfSufficient(10L, 1)).thenReturn(1)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(flashPurchaseRepository.save(any())).thenReturn(
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        )

        // when
        consumer.consume(event)

        // then
        verify(meterRegistry).counter("flash.purchase", "result", "success")
        verify(counter).increment()
    }
}
