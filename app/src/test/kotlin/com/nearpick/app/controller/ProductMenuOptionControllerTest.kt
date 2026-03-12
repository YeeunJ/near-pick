package com.nearpick.app.controller

import com.nearpick.domain.product.ProductMenuOptionService
import com.nearpick.domain.product.dto.MenuChoiceResponse
import com.nearpick.domain.product.dto.ProductMenuOptionGroupResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProductMenuOptionControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var productMenuOptionService: ProductMenuOptionService

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
    fun `POST menu-options - MERCHANT 권한으로 메뉴 옵션을 저장하면 201을 반환한다`() {
        whenever(productMenuOptionService.saveMenuOptions(any(), any(), any())).thenReturn(
            listOf(
                ProductMenuOptionGroupResponse(
                    id = 1L, name = "사이즈", required = true, maxSelect = 1, displayOrder = 0,
                    choices = listOf(
                        MenuChoiceResponse(id = 1L, name = "Small", additionalPrice = 0, displayOrder = 0),
                        MenuChoiceResponse(id = 2L, name = "Large", additionalPrice = 500, displayOrder = 1),
                    ),
                )
            )
        )

        val body = """[{"name":"사이즈","required":true,"maxSelect":1,"displayOrder":0,"choices":[{"name":"Small","additionalPrice":0,"displayOrder":0},{"name":"Large","additionalPrice":500,"displayOrder":1}]}]"""

        mockMvc.post("/api/products/1/menu-options") {
            with(merchantAuth())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data[0].name") { value("사이즈") }
            jsonPath("$.data[0].choices.length()") { value(2) }
        }
    }

    @Test
    fun `POST menu-options - 인증 없으면 403을 반환한다`() {
        mockMvc.post("/api/products/1/menu-options") {
            contentType = MediaType.APPLICATION_JSON
            content = """[{"name":"사이즈","required":true,"maxSelect":1,"displayOrder":0,"choices":[]}]"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE menu-option-group - MERCHANT 권한으로 그룹을 삭제하면 204를 반환한다`() {
        mockMvc.delete("/api/products/1/menu-options/10") {
            with(merchantAuth())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE menu-option-group - 인증 없으면 403을 반환한다`() {
        mockMvc.delete("/api/products/1/menu-options/10").andExpect {
            status { isForbidden() }
        }
    }
}
