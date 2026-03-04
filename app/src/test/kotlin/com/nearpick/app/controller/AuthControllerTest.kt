package com.nearpick.app.controller

import com.nearpick.app.config.JwtTokenProvider
import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.auth.AuthService
import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupResponse
import com.nearpick.domain.user.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var authService: AuthService
    @MockitoBean lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    fun `POST auth-signup-consumer - 정상 요청 시 201을 반환한다`() {
        whenever(authService.signupConsumer(any())).thenReturn(
            SignupResponse(userId = 1L, email = "user@example.com", role = UserRole.CONSUMER)
        )

        mockMvc.post("/api/auth/signup/consumer") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@example.com","password":"pass1234"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun `POST auth-signup-consumer - 이메일 형식이 잘못되면 400을 반환한다`() {
        mockMvc.post("/api/auth/signup/consumer") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"not-an-email","password":"pass1234"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST auth-login - 정상 요청 시 200과 accessToken을 반환한다`() {
        whenever(authService.login(any())).thenReturn(
            LoginResult(userId = 1L, email = "user@example.com", role = UserRole.CONSUMER)
        )
        whenever(jwtTokenProvider.createToken(1L, UserRole.CONSUMER)).thenReturn("mock-jwt-token")

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@example.com","password":"pass1234"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.accessToken") { value("mock-jwt-token") }
        }
    }

    @Test
    fun `POST auth-login - 비밀번호 오류 시 서비스 예외가 GlobalExceptionHandler를 통해 처리된다`() {
        whenever(authService.login(any())).thenThrow(BusinessException(ErrorCode.INVALID_CREDENTIALS))

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@example.com","password":"wrong"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
