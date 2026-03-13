package com.nearpick.nearpick.user.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.user.AdminLevel
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.user.entity.AdminProfileEntity
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.AdminProfileRepository
import com.nearpick.nearpick.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AdminServiceImplTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var adminProfileRepository: AdminProfileRepository

    private lateinit var service: AdminServiceImpl

    private lateinit var consumerUser: UserEntity
    private lateinit var merchantUser: UserEntity
    private lateinit var adminUser: UserEntity
    private lateinit var merchant: MerchantProfileEntity
    private lateinit var product: ProductEntity

    @BeforeEach
    fun setUp() {
        service = AdminServiceImpl(userRepository, productRepository, adminProfileRepository)

        consumerUser = UserEntity(id = 1L, email = "consumer@test.com", passwordHash = "h", role = UserRole.CONSUMER)
        merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        adminUser = UserEntity(id = 3L, email = "admin@test.com", passwordHash = "h", role = UserRole.ADMIN)

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

    // ── getUsers ──────────────────────────────────────────────────────

    @Test
    fun `getUsers - 전체 사용자 목록을 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(consumerUser, merchantUser), pageable, 2)

        whenever(userRepository.searchUsers(role = null, status = null, query = null, pageable = pageable))
            .thenReturn(page)

        val result = service.getUsers(role = null, status = null, query = null, page = 0, size = 20)

        assertEquals(2, result.totalElements)
        assertEquals("consumer@test.com", result.content[0].email)
        assertEquals("merchant@test.com", result.content[1].email)
    }

    @Test
    fun `getUsers - role 필터를 적용하면 해당 역할 사용자만 반환한다`() {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(merchantUser), pageable, 1)

        whenever(userRepository.searchUsers(role = UserRole.MERCHANT, status = null, query = null, pageable = pageable))
            .thenReturn(page)

        val result = service.getUsers(role = UserRole.MERCHANT, status = null, query = null, page = 0, size = 10)

        assertEquals(1, result.totalElements)
        assertEquals(UserRole.MERCHANT, result.content.first().role)
    }

    @Test
    fun `getUsers - 이메일 키워드로 검색하면 해당 사용자를 반환한다`() {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(consumerUser), pageable, 1)

        whenever(userRepository.searchUsers(role = null, status = null, query = "consumer", pageable = pageable))
            .thenReturn(page)

        val result = service.getUsers(role = null, status = null, query = "consumer", page = 0, size = 10)

        assertEquals(1, result.totalElements)
        assertEquals("consumer@test.com", result.content.first().email)
    }

    // ── suspendUser ───────────────────────────────────────────────────

    @Test
    fun `suspendUser - 사용자를 SUSPENDED 상태로 변경한다`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumerUser))

        val result = service.suspendUser(1L)

        assertEquals(UserStatus.SUSPENDED, result.status)
        assertEquals(1L, result.userId)
    }

    @Test
    fun `suspendUser - 존재하지 않는 사용자이면 USER_NOT_FOUND 예외를 던진다`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { service.suspendUser(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    // ── withdrawUser ──────────────────────────────────────────────────

    @Test
    fun `withdrawUser - 사용자를 WITHDRAWN 상태로 변경한다`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(consumerUser))

        val result = service.withdrawUser(1L)

        assertEquals(UserStatus.WITHDRAWN, result.status)
        assertEquals(1L, result.userId)
    }

    @Test
    fun `withdrawUser - 존재하지 않는 사용자이면 USER_NOT_FOUND 예외를 던진다`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { service.withdrawUser(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    // ── getProducts ───────────────────────────────────────────────────

    @Test
    fun `getProducts - 전체 상품 목록을 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(product), pageable, 1)

        whenever(productRepository.findAllByOptionalStatus(status = null, category = null, pageable = pageable))
            .thenReturn(page)

        val result = service.getProducts(status = null, page = 0, size = 20)

        assertEquals(1, result.totalElements)
        assertEquals("테스트 상품", result.content.first().title)
        assertEquals(2L, result.content.first().merchantId)
    }

    @Test
    fun `getProducts - ACTIVE 상태 필터를 적용하면 활성 상품만 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(product), pageable, 1)

        whenever(productRepository.findAllByOptionalStatus(status = ProductStatus.ACTIVE, category = null, pageable = pageable))
            .thenReturn(page)

        val result = service.getProducts(status = ProductStatus.ACTIVE, page = 0, size = 20)

        assertEquals(1, result.totalElements)
        assertEquals(ProductStatus.ACTIVE, result.content.first().status)
    }

    // ── forceCloseProduct ─────────────────────────────────────────────

    @Test
    fun `forceCloseProduct - 상품을 강제 FORCE_CLOSED 상태로 변경한다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = service.forceCloseProduct(1L)

        assertEquals(ProductStatus.FORCE_CLOSED, result.status)
        assertEquals(1L, result.productId)
        assertEquals("테스트 상품", result.title)
    }

    @Test
    fun `forceCloseProduct - 존재하지 않는 상품이면 PRODUCT_NOT_FOUND 예외를 던진다`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { service.forceCloseProduct(99L) }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
    }

    // ── getProfile ────────────────────────────────────────────────────

    @Test
    fun `getProfile - 관리자 프로필을 반환한다`() {
        val adminProfile = AdminProfileEntity(
            userId = 3L,
            user = adminUser,
            adminLevel = AdminLevel.SUPER,
            permissions = """["USER_BAN","PRODUCT_DEACTIVATE"]""",
        )
        whenever(adminProfileRepository.findById(3L)).thenReturn(Optional.of(adminProfile))

        val result = service.getProfile(3L)

        assertEquals(3L, result.adminId)
        assertEquals("admin@test.com", result.email)
        assertEquals(AdminLevel.SUPER, result.adminLevel)
        assertEquals("""["USER_BAN","PRODUCT_DEACTIVATE"]""", result.permissions)
    }

    @Test
    fun `getProfile - 존재하지 않는 관리자이면 USER_NOT_FOUND 예외를 던진다`() {
        whenever(adminProfileRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { service.getProfile(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }
}
