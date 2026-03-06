package com.nearpick.app.controller

import com.nearpick.domain.merchant.MerchantService
import com.nearpick.domain.merchant.dto.MerchantDashboardResponse
import com.nearpick.domain.merchant.dto.MerchantProfileResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class MerchantControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var merchantService: MerchantService

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
    fun `GET api-merchants-me-dashboard - MERCHANT 권한으로 200을 반환한다`() {
        whenever(merchantService.getDashboard(any())).thenReturn(
            MerchantDashboardResponse(
                merchantId = 1L, businessName = "가게",
                totalPopularityScore = 0.0,
                thisMonthReservationCount = 0L,
                thisMonthPurchaseCount = 0L,
                products = emptyList(),
                recentReservations = emptyList(),
            )
        )

        mockMvc.get("/api/merchants/me/dashboard") {
            with(merchantAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.businessName") { value("가게") }
        }
    }

    @Test
    fun `GET api-merchants-me-profile - MERCHANT 권한으로 200을 반환한다`() {
        whenever(merchantService.getProfile(any())).thenReturn(
            MerchantProfileResponse(
                merchantId = 1L,
                email = "merchant@example.com",
                businessName = "가게",
                businessRegNo = "123-45-67890",
                shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
                shopAddress = null,
                rating = BigDecimal("4.5"),
                isVerified = false,
            )
        )

        mockMvc.get("/api/merchants/me/profile") {
            with(merchantAuth())
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `GET api-merchants-me-dashboard - 인증 없으면 403을 반환한다`() {
        mockMvc.get("/api/merchants/me/dashboard").andExpect {
            status { isForbidden() }
        }
    }
}
