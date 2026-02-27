package com.nearpick.app.controller

import com.nearpick.domain.product.ProductService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.product.ProductType
import com.nearpick.domain.product.dto.ProductDetailResponse
import com.nearpick.domain.product.dto.ProductStatusResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var productService: ProductService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun merchantAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_MERCHANT")))
    )

    @Test
    fun `GET api-products-nearby - 인증 없이 200을 반환한다`() {
        whenever(productService.getNearby(any())).thenReturn(PageImpl(emptyList()))

        mockMvc.get("/api/products/nearby") {
            param("lat", "37.5")
            param("lng", "127.0")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun `GET api-products-productId - 인증 없이 200을 반환한다`() {
        whenever(productService.getDetail(1L)).thenReturn(
            ProductDetailResponse(
                id = 1L, title = "상품", description = null,
                price = 1000, productType = ProductType.FLASH_SALE, status = ProductStatus.ACTIVE,
                stock = 5, wishlistCount = 0L,
                shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
                shopAddress = null, merchantName = "가게",
                availableFrom = null, availableUntil = null,
                reservationCount = 0L, purchaseCount = 0L,
            )
        )

        mockMvc.get("/api/products/1").andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(1) }
        }
    }

    @Test
    fun `POST api-products - MERCHANT 권한으로 201을 반환한다`() {
        whenever(productService.create(any(), any())).thenReturn(
            ProductStatusResponse(id = 1L, status = ProductStatus.DRAFT)
        )

        val body = """{"title":"신규 상품","price":1000,"productType":"FLASH_SALE","stock":10}"""

        mockMvc.post("/api/products") {
            with(merchantAuth())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `POST api-products - 인증 없으면 403을 반환한다`() {
        mockMvc.post("/api/products") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"x","price":1000,"productType":"FLASH_SALE","stock":1}"""
        }.andExpect {
            status { isForbidden() }
        }
    }
}
