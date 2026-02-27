package com.nearpick.app.controller

import com.nearpick.domain.transaction.FlashPurchaseService
import com.nearpick.domain.transaction.FlashPurchaseStatus
import com.nearpick.domain.transaction.dto.FlashPurchaseStatusResponse
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class FlashPurchaseControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var flashPurchaseService: FlashPurchaseService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun consumerAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_CONSUMER")))
    )

    @Test
    fun `POST api-flash-purchases - CONSUMER 권한으로 201을 반환한다`() {
        whenever(flashPurchaseService.purchase(any(), any(), any())).thenReturn(
            FlashPurchaseStatusResponse(purchaseId = 1L, status = FlashPurchaseStatus.PENDING)
        )

        mockMvc.post("/api/flash-purchases") {
            with(consumerAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":10,"quantity":1}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun `POST api-flash-purchases - 인증 없으면 403을 반환한다`() {
        mockMvc.post("/api/flash-purchases") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":10,"quantity":1}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-flash-purchases-me - CONSUMER 권한으로 200을 반환한다`() {
        whenever(flashPurchaseService.getMyPurchases(any(), any(), any())).thenReturn(PageImpl(emptyList()))

        mockMvc.get("/api/flash-purchases/me") {
            with(consumerAuth())
        }.andExpect {
            status { isOk() }
        }
    }
}
