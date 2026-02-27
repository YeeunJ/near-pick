package com.nearpick.app.controller

import com.nearpick.domain.admin.AdminService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var adminService: AdminService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    @Test
    fun `GET api-admin-users - ADMIN 권한으로 200을 반환한다`() {
        whenever(adminService.getUsers(any(), any(), any(), any(), any())).thenReturn(PageImpl(emptyList()))

        mockMvc.get("/api/admin/users") {
            with(adminAuth())
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `GET api-admin-users - 인증 없으면 403을 반환한다`() {
        mockMvc.get("/api/admin/users").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-admin-products - ADMIN 권한으로 200을 반환한다`() {
        whenever(adminService.getProducts(any(), any(), any())).thenReturn(PageImpl(emptyList()))

        mockMvc.get("/api/admin/products") {
            with(adminAuth())
        }.andExpect {
            status { isOk() }
        }
    }
}
