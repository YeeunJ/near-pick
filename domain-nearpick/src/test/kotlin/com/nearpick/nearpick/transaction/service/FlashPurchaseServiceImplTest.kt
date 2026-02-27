package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.transaction.dto.FlashPurchaseCreateRequest
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
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class FlashPurchaseServiceImplTest {

    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository

    @InjectMocks lateinit var flashPurchaseService: FlashPurchaseServiceImpl

    private lateinit var user: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var activeProduct: ProductEntity

    @BeforeEach
    fun setUp() {
        user = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)

        val merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )

        activeProduct = ProductEntity(
            id = 10L, merchant = merchant, title = "Flash Item",
            price = 1000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    @Test
    fun `purchase - 재고가 충분하면 재고를 차감하고 구매를 저장한다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 2)
        val savedPurchase = FlashPurchaseEntity(id = 1L, user = user, product = activeProduct, quantity = 2)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(activeProduct)
        whenever(flashPurchaseRepository.save(any())).thenReturn(savedPurchase)

        // when
        val response = flashPurchaseService.purchase(1L, 10L, request)

        // then
        assertEquals(5 - 2, activeProduct.stock)  // 재고 차감 확인
    }

    @Test
    fun `purchase - 재고가 부족하면 OUT_OF_STOCK 예외를 던진다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 10)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(activeProduct)

        // when / then
        val ex = assertThrows<BusinessException> { flashPurchaseService.purchase(1L, 10L, request) }
        assertEquals(ErrorCode.OUT_OF_STOCK, ex.errorCode)
    }

    @Test
    fun `purchase - 상품이 비활성 상태이면 PRODUCT_NOT_ACTIVE 예외를 던진다`() {
        // given
        val inactiveProduct = activeProduct.apply { status = ProductStatus.CLOSED }
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 1)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findByIdWithLock(10L)).thenReturn(inactiveProduct)

        // when / then
        val ex = assertThrows<BusinessException> { flashPurchaseService.purchase(1L, 10L, request) }
        assertEquals(ErrorCode.PRODUCT_NOT_ACTIVE, ex.errorCode)
    }

    @Test
    fun `purchase - 상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 99L, quantity = 1)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(productRepository.findByIdWithLock(99L)).thenReturn(null)

        // when / then
        val ex = assertThrows<BusinessException> { flashPurchaseService.purchase(1L, 99L, request) }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `purchase - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다`() {
        // given
        val request = FlashPurchaseCreateRequest(productId = 10L, quantity = 1)
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        // when / then
        val ex = assertThrows<BusinessException> { flashPurchaseService.purchase(99L, 10L, request) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }
}
