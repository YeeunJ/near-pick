package com.nearpick.nearpick.transaction.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.entity.WishlistEntity
import com.nearpick.nearpick.transaction.repository.WishlistRepository
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
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class WishlistServiceImplTest {

    @Mock lateinit var wishlistRepository: WishlistRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository

    @InjectMocks lateinit var wishlistService: WishlistServiceImpl

    private lateinit var consumer: UserEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        consumer = UserEntity(id = 1L, email = "consumer@example.com", passwordHash = "h", role = UserRole.CONSUMER)

        val merchantUser = UserEntity(id = 2L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        val merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "Shop",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
        product = ProductEntity(
            id = 10L, merchant = merchant, title = "Test Product",
            price = 1000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    @Test
    fun `add - 찜이 없으면 저장하고 wishlistId를 반환한다`() {
        val saved = WishlistEntity(id = 1L, user = consumer, product = product)

        whenever(wishlistRepository.existsByUser_IdAndProduct_Id(1L, 10L)).thenReturn(false)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(wishlistRepository.save(any())).thenReturn(saved)

        val result = wishlistService.add(1L, 10L)

        assertEquals(1L, result)
    }

    @Test
    fun `add - 이미 찜한 경우 ALREADY_WISHLISTED 예외를 던진다`() {
        whenever(wishlistRepository.existsByUser_IdAndProduct_Id(1L, 10L)).thenReturn(true)

        val ex = assertThrows<BusinessException> { wishlistService.add(1L, 10L) }
        assertEquals(ErrorCode.ALREADY_WISHLISTED, ex.errorCode)
        verify(wishlistRepository, never()).save(any())
    }

    @Test
    fun `add - 상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        whenever(wishlistRepository.existsByUser_IdAndProduct_Id(1L, 99L)).thenReturn(false)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { wishlistService.add(1L, 99L) }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `remove - 찜이 존재하면 삭제한다`() {
        val wishlist = WishlistEntity(id = 1L, user = consumer, product = product)
        whenever(wishlistRepository.findByUser_IdAndProduct_Id(1L, 10L)).thenReturn(wishlist)

        wishlistService.remove(1L, 10L)

        verify(wishlistRepository).delete(wishlist)
    }

    @Test
    fun `remove - 찜이 없으면 RESOURCE_NOT_FOUND 예외를 던진다`() {
        whenever(wishlistRepository.findByUser_IdAndProduct_Id(1L, 10L)).thenReturn(null)

        val ex = assertThrows<BusinessException> { wishlistService.remove(1L, 10L) }
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `getMyWishlists - 최대 200개 찜 목록을 반환한다`() {
        val wishlists = listOf(WishlistEntity(id = 1L, user = consumer, product = product))
        whenever(wishlistRepository.findTop200ByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(wishlists)

        val result = wishlistService.getMyWishlists(1L)

        assertEquals(1, result.size)
        assertEquals(10L, result[0].productId)
    }
}
