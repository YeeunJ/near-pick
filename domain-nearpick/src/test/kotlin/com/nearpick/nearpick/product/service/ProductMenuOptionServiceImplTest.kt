package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.MenuChoiceRequest
import com.nearpick.domain.product.dto.MenuOptionGroupRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.entity.ProductEntity
import com.nearpick.nearpick.product.entity.ProductMenuChoiceEntity
import com.nearpick.nearpick.product.entity.ProductMenuOptionGroupEntity
import com.nearpick.nearpick.product.repository.ProductMenuOptionGroupRepository
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ProductMenuOptionServiceImplTest {

    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var menuOptionGroupRepository: ProductMenuOptionGroupRepository

    private lateinit var service: ProductMenuOptionServiceImpl

    private lateinit var merchant: MerchantProfileEntity
    private lateinit var foodProduct: ProductEntity
    private lateinit var beautyProduct: ProductEntity

    @BeforeEach
    fun setUp() {
        service = ProductMenuOptionServiceImpl(productRepository, menuOptionGroupRepository)

        val merchantUser = UserEntity(id = 2L, email = "merchant@test.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchant = MerchantProfileEntity(
            userId = 2L, user = merchantUser, businessName = "테스트샵",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
        )
        foodProduct = ProductEntity(
            id = 1L, merchant = merchant, title = "음식 상품",
            price = 5000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 10,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
            category = ProductCategory.FOOD,
        )
        beautyProduct = ProductEntity(
            id = 2L, merchant = merchant, title = "뷰티 상품",
            price = 15000, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
            category = ProductCategory.BEAUTY,
        )
    }

    // ── saveMenuOptions ───────────────────────────────────────────────

    @Test
    fun `saveMenuOptions - FOOD 카테고리 상품에 메뉴 옵션을 저장한다`() {
        val groups = listOf(
            MenuOptionGroupRequest(
                name = "사이즈",
                required = true,
                maxSelect = 1,
                displayOrder = 0,
                choices = listOf(
                    MenuChoiceRequest(name = "Small", additionalPrice = 0, displayOrder = 0),
                    MenuChoiceRequest(name = "Large", additionalPrice = 500, displayOrder = 1),
                ),
            ),
        )
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(foodProduct))
        whenever(menuOptionGroupRepository.save(any())).thenAnswer { it.arguments[0] as ProductMenuOptionGroupEntity }

        val result = service.saveMenuOptions(merchantId = 2L, productId = 1L, groups = groups)

        assertEquals(1, result.size)
        assertEquals("사이즈", result[0].name)
        assertEquals(true, result[0].required)
        assertEquals(2, result[0].choices.size)
        assertEquals("Small", result[0].choices[0].name)
        assertEquals(500, result[0].choices[1].additionalPrice)
        verify(menuOptionGroupRepository).deleteAllByProductId(1L)
    }

    @Test
    fun `saveMenuOptions - BEVERAGE 카테고리 상품에도 메뉴 옵션을 저장할 수 있다`() {
        val beverageProduct = ProductEntity(
            id = 3L, merchant = merchant, title = "음료 상품",
            price = 4500, productType = ProductType.RESERVATION,
            status = ProductStatus.ACTIVE, stock = 20,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
            category = ProductCategory.BEVERAGE,
        )
        val groups = listOf(
            MenuOptionGroupRequest(name = "온도", required = true, maxSelect = 1, displayOrder = 0),
        )
        whenever(productRepository.findById(3L)).thenReturn(Optional.of(beverageProduct))
        whenever(menuOptionGroupRepository.save(any())).thenAnswer { it.arguments[0] as ProductMenuOptionGroupEntity }

        val result = service.saveMenuOptions(merchantId = 2L, productId = 3L, groups = groups)

        assertEquals(1, result.size)
    }

    @Test
    fun `saveMenuOptions - 소유자가 아니면 FORBIDDEN 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(foodProduct))

        val ex = assertThrows<BusinessException> {
            service.saveMenuOptions(merchantId = 99L, productId = 1L, groups = emptyList())
        }
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `saveMenuOptions - BEAUTY 카테고리이면 MENU_OPTION_NOT_ALLOWED 예외를 던진다`() {
        whenever(productRepository.findById(2L)).thenReturn(Optional.of(beautyProduct))

        val ex = assertThrows<BusinessException> {
            service.saveMenuOptions(merchantId = 2L, productId = 2L, groups = emptyList())
        }
        assertEquals(ErrorCode.MENU_OPTION_NOT_ALLOWED, ex.errorCode)
    }

    @Test
    fun `saveMenuOptions - 카테고리가 null이면 MENU_OPTION_NOT_ALLOWED 예외를 던진다`() {
        val noCategory = ProductEntity(
            id = 4L, merchant = merchant, title = "카테고리 없는 상품",
            price = 1000, productType = ProductType.FLASH_SALE,
            status = ProductStatus.ACTIVE, stock = 5,
            shopLat = BigDecimal("37.5665"), shopLng = BigDecimal("126.9780"),
            category = null,
        )
        whenever(productRepository.findById(4L)).thenReturn(Optional.of(noCategory))

        val ex = assertThrows<BusinessException> {
            service.saveMenuOptions(merchantId = 2L, productId = 4L, groups = emptyList())
        }
        assertEquals(ErrorCode.MENU_OPTION_NOT_ALLOWED, ex.errorCode)
    }

    // ── deleteMenuOptionGroup ─────────────────────────────────────────

    @Test
    fun `deleteMenuOptionGroup - 성공 시 그룹을 삭제한다`() {
        val group = ProductMenuOptionGroupEntity(
            id = 10L, product = foodProduct, name = "사이즈",
            required = true, maxSelect = 1, displayOrder = 0,
        )
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(foodProduct))
        whenever(menuOptionGroupRepository.findByIdAndProductId(10L, 1L)).thenReturn(group)

        service.deleteMenuOptionGroup(merchantId = 2L, productId = 1L, groupId = 10L)

        verify(menuOptionGroupRepository).delete(group)
    }

    @Test
    fun `deleteMenuOptionGroup - 존재하지 않는 그룹이면 MENU_OPTION_GROUP_NOT_FOUND 예외를 던진다`() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(foodProduct))
        whenever(menuOptionGroupRepository.findByIdAndProductId(99L, 1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            service.deleteMenuOptionGroup(merchantId = 2L, productId = 1L, groupId = 99L)
        }
        assertEquals(ErrorCode.MENU_OPTION_GROUP_NOT_FOUND, ex.errorCode)
    }

    // ── getMenuOptions ────────────────────────────────────────────────

    @Test
    fun `getMenuOptions - 상품의 메뉴 옵션 목록을 순서대로 반환한다`() {
        val group1 = ProductMenuOptionGroupEntity(
            id = 1L, product = foodProduct, name = "사이즈",
            required = true, maxSelect = 1, displayOrder = 0,
        ).also { g ->
            g.choices.add(ProductMenuChoiceEntity(id = 1L, group = g, name = "Small", additionalPrice = 0, displayOrder = 0))
            g.choices.add(ProductMenuChoiceEntity(id = 2L, group = g, name = "Large", additionalPrice = 500, displayOrder = 1))
        }
        val group2 = ProductMenuOptionGroupEntity(
            id = 2L, product = foodProduct, name = "온도",
            required = false, maxSelect = 1, displayOrder = 1,
        )
        whenever(menuOptionGroupRepository.findAllByProductIdOrderByDisplayOrder(1L))
            .thenReturn(listOf(group1, group2))

        val result = service.getMenuOptions(1L)

        assertEquals(2, result.size)
        assertEquals("사이즈", result[0].name)
        assertEquals(2, result[0].choices.size)
        assertEquals("온도", result[1].name)
    }
}
