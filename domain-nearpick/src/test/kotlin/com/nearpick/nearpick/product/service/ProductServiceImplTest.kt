package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.SortType
import com.nearpick.domain.product.dto.ProductNearbyRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductNearbyProjection
import com.nearpick.nearpick.product.repository.ProductRepository
import org.mockito.kotlin.mock
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.transaction.repository.WishlistRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ProductServiceImplTest {

    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var merchantProfileRepository: MerchantProfileRepository
    @Mock lateinit var wishlistRepository: WishlistRepository
    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository

    @InjectMocks lateinit var productService: ProductServiceImpl

    private lateinit var merchantUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "테스트샵",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
        )
        product = ProductEntity(
            id = 1L, merchant = merchant, title = "테스트 상품",
            price = 5000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
        )
    }

    // ── getDetail ────────────────────────────────────────────────────

    @Test
    fun `getDetail - 존재하는 상품 ID로 조회하면 상세 정보를 반환한다`() {
        // given
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(wishlistRepository.countByProduct_Id(1L)).thenReturn(5L)
        whenever(reservationRepository.countByProduct_Id(1L)).thenReturn(3L)
        whenever(flashPurchaseRepository.countByProduct_Id(1L)).thenReturn(2L)

        // when
        val result = productService.getDetail(1L)

        // then
        assertEquals(1L, result.id)
        assertEquals("테스트 상품", result.title)
        assertEquals(5L, result.wishlistCount)
        assertEquals(3L, result.reservationCount)
        assertEquals(2L, result.purchaseCount)
    }

    @Test
    fun `getDetail - 존재하지 않는 상품 ID로 조회하면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        // given
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        // when / then
        val ex = assertThrows<BusinessException> { productService.getDetail(99L) }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    // ── getNearby ────────────────────────────────────────────────────

    @Test
    fun `getNearby - 위치 기반 조회 결과를 반환한다`() {
        // given
        val request = ProductNearbyRequest(
            lat = BigDecimal("37.5665"),
            lng = BigDecimal("126.9780"),
            radius = 5.0,
            sort = SortType.POPULARITY,
            page = 0,
            size = 10,
        )
        val pageable = PageRequest.of(0, 10)
        val projection = mock<ProductNearbyProjection> {
            on { id } doReturn 1L
            on { title } doReturn "테스트 상품"
            on { price } doReturn 5000
            on { productType } doReturn "FLASH_SALE"
            on { status } doReturn "ACTIVE"
            on { popularityScore } doReturn BigDecimal.ZERO
            on { distanceKm } doReturn 0.5
            on { merchantName } doReturn "테스트샵"
            on { shopAddress } doReturn null
            on { shopLat } doReturn BigDecimal("37.5665")
            on { shopLng } doReturn BigDecimal("126.9780")
        }
        val page = PageImpl(listOf(projection), pageable, 1)

        whenever(productRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radius = 5.0,
            sort = "popularity",
            pageable = pageable,
        )).thenReturn(page)

        // when
        val result = productService.getNearby(request)

        // then
        assertEquals(1, result.totalElements)
        assertEquals("테스트 상품", result.content.first().title)
    }

    // ── getMyProducts ─────────────────────────────────────────────────

    @Test
    fun `getMyProducts - 판매자 상품 목록을 조회한다`() {
        // given
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(product), pageable, 1)

        whenever(productRepository.findAllByMerchant_UserId(2L, pageable)).thenReturn(page)
        whenever(wishlistRepository.countByProductIds(listOf(1L))).thenReturn(emptyList())

        // when
        val result = productService.getMyProducts(merchantId = 2L, page = 0, size = 10)

        // then
        assertEquals(1, result.totalElements)
        assertEquals("테스트 상품", result.content.first().title)
    }
}
