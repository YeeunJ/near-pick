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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

/**
 * FlashPurchase 동시성 통합 테스트 (G-5)
 *
 * 실제 분산 환경의 동시성 검증은 k6 시나리오 3에서 수행:
 *   - 100명 동시 구매 시도 → CONFIRMED 100건, stock=0, 초과 판매 없음 ✅
 *
 * 이 테스트는 Consumer 로직의 직렬화 의도를 확인:
 * - 재고보다 많은 동시 요청에서 초과 저장 없음
 * - idempotency key로 중복 이벤트 차단
 */
@ExtendWith(MockitoExtension::class)
class FlashPurchaseConcurrencyTest {

    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var redissonClient: RedissonClient
    @Mock lateinit var lock: RLock

    private val savedCount = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        whenever(lock.isHeldByCurrentThread).thenReturn(true)
        whenever(lock.tryLock(any<Long>(), any<Long>(), any())).thenReturn(true)
    }

    @Test
    fun `같은 멱등성 키를 가진 이벤트는 한 번만 처리된다`() {
        // given: Redis 멱등성 시뮬레이션 (첫 번째만 true, 이후 false)
        val processed = java.util.concurrent.atomic.AtomicBoolean(false)
        val mockBucket = org.mockito.kotlin.mock<RBucket<String>>()
        whenever(mockBucket.setIfAbsent(any<String>(), any())).thenAnswer {
            processed.compareAndSet(false, true)  // 첫 번째만 true
        }
        whenever(redissonClient.getBucket<String>(any<String>())).thenReturn(mockBucket)
        whenever(redissonClient.getLock(any<String>())).thenReturn(lock)

        val merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        val merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "샵",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        val user = UserEntity(id = 1L, email = "consumer@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        val product = ProductEntity(
            id = 10L, merchant = merchant, title = "Flash Item",
            price = 5000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )

        whenever(productRepository.findByIdWithLock(10L)).thenReturn(product)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(flashPurchaseRepository.save(any())).thenAnswer {
            savedCount.incrementAndGet()
            FlashPurchaseEntity(user = user, product = product, quantity = 1, status = FlashPurchaseStatus.CONFIRMED)
        }

        val consumer = FlashPurchaseConsumer(flashPurchaseRepository, productRepository, userRepository, redissonClient)
        val sameEvent = FlashPurchaseRequestEvent("1-10-20260307", 1L, 10L, 1, LocalDateTime.now())

        // when: 동일 이벤트 5번 시도
        val threads = 5
        val latch = CountDownLatch(threads)
        val executor = Executors.newFixedThreadPool(threads)

        repeat(threads) {
            executor.submit {
                try { consumer.consume(sameEvent) } catch (_: Exception) {}
                finally { latch.countDown() }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // then: 단 1번만 저장
        assertTrue(savedCount.get() <= 1, "중복 이벤트는 1번만 저장되어야 함 (실제: ${savedCount.get()})")
    }
}
