package com.nearpick.nearpick.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.LocationSource
import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductImageService
import com.nearpick.domain.product.ProductMenuOptionService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.SortType
import com.nearpick.domain.product.dto.ProductCreateRequest
import com.nearpick.domain.product.dto.ProductNearbyRequest
import com.nearpick.domain.product.dto.ProductSpecItem
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.location.entity.SavedLocationEntity
import com.nearpick.nearpick.location.repository.SavedLocationRepository
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductNearbyProjection
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.transaction.repository.WishlistRepository
import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RAtomicLong
import org.redisson.api.RedissonClient
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
    @Mock lateinit var consumerProfileRepository: ConsumerProfileRepository
    @Mock lateinit var savedLocationRepository: SavedLocationRepository
    @Mock lateinit var productImageService: ProductImageService
    @Mock lateinit var productMenuOptionService: ProductMenuOptionService
    @Mock lateinit var redissonClient: RedissonClient

    private val objectMapper = ObjectMapper().apply { registerModule(kotlinModule()) }
    private lateinit var productService: ProductServiceImpl

    private lateinit var merchantUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        productService = ProductServiceImpl(
            productRepository = productRepository,
            merchantProfileRepository = merchantProfileRepository,
            wishlistRepository = wishlistRepository,
            reservationRepository = reservationRepository,
            flashPurchaseRepository = flashPurchaseRepository,
            consumerProfileRepository = consumerProfileRepository,
            savedLocationRepository = savedLocationRepository,
            productImageService = productImageService,
            productMenuOptionService = productMenuOptionService,
            objectMapper = objectMapper,
            redissonClient = redissonClient,
        )

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
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(wishlistRepository.countByProduct_Id(1L)).thenReturn(5L)
        whenever(reservationRepository.countByProduct_Id(1L)).thenReturn(3L)
        whenever(flashPurchaseRepository.countByProduct_Id(1L)).thenReturn(2L)
        whenever(productImageService.getImages(1L)).thenReturn(emptyList())
        whenever(productMenuOptionService.getMenuOptions(1L)).thenReturn(emptyList())

        val result = productService.getDetail(1L)

        assertEquals(1L, result.id)
        assertEquals("테스트 상품", result.title)
        assertEquals(5L, result.wishlistCount)
        assertEquals(3L, result.reservationCount)
        assertEquals(2L, result.purchaseCount)
    }

    @Test
    fun `getDetail - 존재하지 않는 상품 ID로 조회하면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { productService.getDetail(99L) }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    // ── getNearby ────────────────────────────────────────────────────

    @Test
    fun `getNearby - 위치 기반 조회 결과를 반환한다`() {
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
            on { category } doReturn null
            on { thumbnailUrl } doReturn null
        }
        val page = PageImpl(listOf(projection), pageable, 1)

        whenever(productRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radius = 5.0,
            sort = "popularity",
            category = null,
            pageable = pageable,
        )).thenReturn(page)

        val result = productService.getNearby(request)

        assertEquals(1, result.totalElements)
        assertEquals("테스트 상품", result.content.first().title)
        assertNull(result.content.first().thumbnailUrl)
    }

    @Test
    fun `getNearby - 이미지가 있는 상품의 thumbnailUrl이 응답에 포함된다`() {
        val thumbnailUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/products/1/images/thumb.jpg"
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
            on { title } doReturn "썸네일 상품"
            on { price } doReturn 5000
            on { productType } doReturn "FLASH_SALE"
            on { status } doReturn "ACTIVE"
            on { popularityScore } doReturn BigDecimal.ZERO
            on { distanceKm } doReturn 0.3
            on { merchantName } doReturn "테스트샵"
            on { shopAddress } doReturn null
            on { shopLat } doReturn BigDecimal("37.5665")
            on { shopLng } doReturn BigDecimal("126.9780")
            on { category } doReturn null
            on { this.thumbnailUrl } doReturn thumbnailUrl
        }
        val page = PageImpl(listOf(projection), pageable, 1)

        whenever(productRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radius = 5.0,
            sort = "popularity",
            category = null,
            pageable = pageable,
        )).thenReturn(page)

        val result = productService.getNearby(request)

        assertNotNull(result.content.first().thumbnailUrl)
        assertEquals(thumbnailUrl, result.content.first().thumbnailUrl)
    }

    // ── getMyProducts ─────────────────────────────────────────────────

    @Test
    fun `getMyProducts - 판매자 상품 목록을 조회한다`() {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(product), pageable, 1)

        whenever(productRepository.findAllByMerchant_UserId(2L, pageable)).thenReturn(page)
        whenever(wishlistRepository.countByProductIds(listOf(1L))).thenReturn(emptyList())

        val result = productService.getMyProducts(merchantId = 2L, page = 0, size = 10)

        assertEquals(1, result.totalElements)
        assertEquals("테스트 상품", result.content.first().title)
    }

    // ── getNearby (category / locationSource) ─────────────────────────

    @Test
    fun `getNearby - category 필터를 포함해 FOOD 상품만 조회한다`() {
        val request = ProductNearbyRequest(
            lat = BigDecimal("37.5665"),
            lng = BigDecimal("126.9780"),
            radius = 5.0,
            sort = SortType.POPULARITY,
            page = 0,
            size = 10,
            category = ProductCategory.FOOD,
        )
        val pageable = PageRequest.of(0, 10)
        val projection = mock<ProductNearbyProjection> {
            on { id } doReturn 2L
            on { title } doReturn "음식 상품"
            on { price } doReturn 8000
            on { productType } doReturn "RESERVATION"
            on { status } doReturn "ACTIVE"
            on { popularityScore } doReturn java.math.BigDecimal.ZERO
            on { distanceKm } doReturn 1.2
            on { merchantName } doReturn "테스트샵"
            on { shopAddress } doReturn null
            on { shopLat } doReturn BigDecimal("37.5665")
            on { shopLng } doReturn BigDecimal("126.9780")
            on { category } doReturn "FOOD"
            on { thumbnailUrl } doReturn null
        }
        val page = PageImpl(listOf(projection), pageable, 1)

        whenever(productRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radius = 5.0,
            sort = "popularity",
            category = "FOOD",
            pageable = pageable,
        )).thenReturn(page)

        val result = productService.getNearby(request)

        assertEquals(1, result.totalElements)
        assertEquals(ProductCategory.FOOD, result.content.first().category)
    }

    @Test
    fun `getNearby - CURRENT locationSource로 소비자 현재 위치를 사용한다`() {
        val consumerUser = UserEntity(id = 3L, email = "consumer@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        val consumer = ConsumerProfileEntity(
            userId = 3L, user = consumerUser, nickname = "소비자",
            currentLat = BigDecimal("37.4979"), currentLng = BigDecimal("127.0276"),
        )
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<ProductNearbyProjection>(emptyList(), pageable, 0)

        whenever(consumerProfileRepository.findById(3L)).thenReturn(Optional.of(consumer))
        whenever(productRepository.findNearby(
            lat = 37.4979,
            lng = 127.0276,
            radius = 3.0,
            sort = "distance",
            category = null,
            pageable = pageable,
        )).thenReturn(emptyPage)

        val request = ProductNearbyRequest(
            locationSource = LocationSource.CURRENT,
            radius = 3.0,
            sort = SortType.DISTANCE,
            page = 0,
            size = 10,
        )

        val result = productService.getNearby(request, userId = 3L)

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `getNearby - SAVED locationSource로 저장된 위치를 사용한다`() {
        val consumerUser = UserEntity(id = 3L, email = "consumer@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        val consumer = ConsumerProfileEntity(userId = 3L, user = consumerUser, nickname = "소비자")
        val savedLocation = SavedLocationEntity(
            id = 5L, consumer = consumer, label = "집",
            lat = BigDecimal("37.5500"), lng = BigDecimal("127.0000"),
        )
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<ProductNearbyProjection>(emptyList(), pageable, 0)

        whenever(savedLocationRepository.findByIdAndConsumerUserId(5L, 3L)).thenReturn(savedLocation)
        whenever(productRepository.findNearby(
            lat = 37.55,
            lng = 127.0,
            radius = 5.0,
            sort = "popularity",
            category = null,
            pageable = pageable,
        )).thenReturn(emptyPage)

        val request = ProductNearbyRequest(
            locationSource = LocationSource.SAVED,
            savedLocationId = 5L,
            radius = 5.0,
            sort = SortType.POPULARITY,
            page = 0,
            size = 10,
        )

        val result = productService.getNearby(request, userId = 3L)

        assertEquals(0, result.totalElements)
    }

    // ── getDetail (specs) ─────────────────────────────────────────────

    @Test
    fun `getDetail - specs JSON을 파싱하여 상품 스펙 정보를 반환한다`() {
        val productWithSpecs = ProductEntity(
            id = 2L, merchant = merchant, title = "스펙 상품",
            price = 3000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
            specs = """[{"key":"원산지","value":"국내산"},{"key":"중량","value":"500g"}]""",
        )
        whenever(productRepository.findById(2L)).thenReturn(Optional.of(productWithSpecs))
        whenever(wishlistRepository.countByProduct_Id(2L)).thenReturn(0L)
        whenever(reservationRepository.countByProduct_Id(2L)).thenReturn(0L)
        whenever(flashPurchaseRepository.countByProduct_Id(2L)).thenReturn(0L)
        whenever(productImageService.getImages(2L)).thenReturn(emptyList())
        whenever(productMenuOptionService.getMenuOptions(2L)).thenReturn(emptyList())

        val result = productService.getDetail(2L)

        assertEquals(2L, result.id)
        assertEquals(2, result.specs?.size)
        assertEquals("원산지", result.specs?.get(0)?.key)
        assertEquals("국내산", result.specs?.get(0)?.value)
        assertEquals("중량", result.specs?.get(1)?.key)
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    fun `create - category와 specs를 포함해 상품을 생성한다`() {
        val specs = listOf(ProductSpecItem(key = "원산지", value = "국내산"))
        val request = ProductCreateRequest(
            title = "음식 상품",
            price = 8000,
            productType = ProductType.RESERVATION,
            stock = 20,
            category = ProductCategory.FOOD,
            specs = specs,
        )
        whenever(merchantProfileRepository.findById(2L)).thenReturn(Optional.of(merchant))
        whenever(productRepository.save(any())).thenReturn(product)

        val result = productService.create(merchantId = 2L, request = request)

        assertEquals(1L, result.id)
        assertEquals(ProductStatus.ACTIVE, result.status)
    }

    // ── close ─────────────────────────────────────────────────────────

    @Test
    fun `close - 상품을 CLOSED 상태로 변경한다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.close(merchantId = 2L, productId = 1L)

        assertEquals(ProductStatus.CLOSED, result.status)
    }

    @Test
    fun `close - 소유자가 아니면 FORBIDDEN 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> { productService.close(merchantId = 99L, productId = 1L) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    // ── pauseProduct (Phase 12) ────────────────────────────────────────

    @Test
    fun `pauseProduct - ACTIVE 상품을 PAUSED로 변경한다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.pauseProduct(merchantId = 2L, productId = 1L)

        assertEquals(ProductStatus.PAUSED, result.status)
    }

    @Test
    fun `pauseProduct - ACTIVE가 아닌 상품은 PRODUCT_CANNOT_BE_PAUSED 예외를 던진다`() {
        product.status = ProductStatus.PAUSED
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> { productService.pauseProduct(merchantId = 2L, productId = 1L) }
        assertEquals(ErrorCode.PRODUCT_CANNOT_BE_PAUSED, ex.errorCode)
    }

    // ── resumeProduct (Phase 12) ───────────────────────────────────────

    @Test
    fun `resumeProduct - PAUSED 상품을 ACTIVE로 변경한다`() {
        product.status = ProductStatus.PAUSED
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.resumeProduct(merchantId = 2L, productId = 1L)

        assertEquals(ProductStatus.ACTIVE, result.status)
    }

    @Test
    fun `resumeProduct - PAUSED가 아닌 상품은 PRODUCT_CANNOT_BE_RESUMED 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> { productService.resumeProduct(merchantId = 2L, productId = 1L) }
        assertEquals(ErrorCode.PRODUCT_CANNOT_BE_RESUMED, ex.errorCode)
    }

    // ── addStock (Phase 12) ────────────────────────────────────────────

    @Test
    fun `addStock - 재고를 추가하고 PAUSED 상품을 자동 ACTIVE로 복원한다`() {
        val atomicLong = mock<RAtomicLong>()
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))
        whenever(redissonClient.getAtomicLong("stock:flash:1")).thenReturn(atomicLong)

        productService.addStock(merchantId = 2L, productId = 1L, additionalStock = 10)

        verify(productRepository).incrementStock(1L, 10)
        verify(productRepository).resumeIfRestored(1L)
        verify(atomicLong).addAndGet(10L)
    }

    @Test
    fun `addStock - FORCE_CLOSED 상품에 재고 추가 시 FORBIDDEN 예외를 던진다`() {
        product.status = ProductStatus.FORCE_CLOSED
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val ex = assertThrows<BusinessException> { productService.addStock(merchantId = 2L, productId = 1L, additionalStock = 5) }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }
}
