package com.nearpick.nearpick.product.scheduler

import com.nearpick.nearpick.product.repository.ProductRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RAtomicLong
import org.redisson.api.RKeys
import org.redisson.api.RedissonClient
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductSchedulerTest {

    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var redissonClient: RedissonClient

    @InjectMocks lateinit var scheduler: ProductScheduler

    @Test
    fun `pauseExpiredProducts - availableUntil이 만료된 ACTIVE 상품을 PAUSED로 변경한다`() {
        // given
        whenever(productRepository.pauseExpiredProducts(any())).thenReturn(3)

        // when
        scheduler.pauseExpiredProducts()

        // then
        verify(productRepository).pauseExpiredProducts(any())
    }

    @Test
    fun `pauseExpiredProducts - 만료된 상품이 없으면 로그만 남기지 않는다`() {
        // given
        whenever(productRepository.pauseExpiredProducts(any())).thenReturn(0)

        // when — should not throw
        scheduler.pauseExpiredProducts()

        // then
        verify(productRepository).pauseExpiredProducts(any())
    }

    @Test
    fun `syncRedisStockWithDb - Redis와 DB 재고가 일치하면 수정하지 않는다`() {
        // given
        val keys = mockRKeys(listOf("stock:flash:10"))
        val atomicLong = org.mockito.kotlin.mock<RAtomicLong>()
        whenever(redissonClient.keys).thenReturn(keys)
        whenever(redissonClient.getAtomicLong("stock:flash:10")).thenReturn(atomicLong)
        whenever(atomicLong.get()).thenReturn(5L)
        val product = com.nearpick.nearpick.product.entity.ProductEntity(
            id = 10L, merchant = org.mockito.kotlin.mock(), title = "Item",
            price = 1000, productType = com.nearpick.domain.product.ProductType.FLASH_SALE,
            status = com.nearpick.domain.product.ProductStatus.ACTIVE, stock = 5,
            shopLat = java.math.BigDecimal("37.5"), shopLng = java.math.BigDecimal("127.0"),
        )
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))

        // when
        scheduler.syncRedisStockWithDb()

        // then — no reset called
        verify(atomicLong, org.mockito.kotlin.never()).set(any())
    }

    @Test
    fun `syncRedisStockWithDb - Redis와 DB 재고가 다르면 DB 기준으로 Redis를 재설정한다`() {
        // given
        val keys = mockRKeys(listOf("stock:flash:10"))
        val atomicLong = org.mockito.kotlin.mock<RAtomicLong>()
        whenever(redissonClient.keys).thenReturn(keys)
        whenever(redissonClient.getAtomicLong("stock:flash:10")).thenReturn(atomicLong)
        whenever(atomicLong.get()).thenReturn(99L)   // Redis 값 불일치
        val product = com.nearpick.nearpick.product.entity.ProductEntity(
            id = 10L, merchant = org.mockito.kotlin.mock(), title = "Item",
            price = 1000, productType = com.nearpick.domain.product.ProductType.FLASH_SALE,
            status = com.nearpick.domain.product.ProductStatus.ACTIVE, stock = 5,
            shopLat = java.math.BigDecimal("37.5"), shopLng = java.math.BigDecimal("127.0"),
        )
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))

        // when
        scheduler.syncRedisStockWithDb()

        // then — Redis reset to DB value (5)
        verify(atomicLong).set(5L)
    }

    private fun mockRKeys(keys: List<String>): RKeys {
        val rKeys = org.mockito.kotlin.mock<RKeys>()
        whenever(rKeys.getKeysByPattern("stock:flash:*")).thenReturn(keys)
        return rKeys
    }
}
