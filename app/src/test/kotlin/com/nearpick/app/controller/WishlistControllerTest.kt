package com.nearpick.app.controller

import com.nearpick.domain.transaction.WishlistService
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class WishlistControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var wishlistService: WishlistService

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
    fun `POST api-wishlists - CONSUMER 권한으로 201을 반환한다`() {
        whenever(wishlistService.add(any(), any())).thenReturn(1L)

        mockMvc.post("/api/wishlists") {
            with(consumerAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":10}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.wishlistId") { value(1) }
        }
    }

    @Test
    fun `GET api-wishlists-me - CONSUMER 권한으로 200을 반환한다`() {
        whenever(wishlistService.getMyWishlists(any())).thenReturn(emptyList())

        mockMvc.get("/api/wishlists/me") {
            with(consumerAuth())
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST api-wishlists - 인증 없으면 403을 반환한다`() {
        mockMvc.post("/api/wishlists") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":10}"""
        }.andExpect {
            status { isForbidden() }
        }
    }
}
