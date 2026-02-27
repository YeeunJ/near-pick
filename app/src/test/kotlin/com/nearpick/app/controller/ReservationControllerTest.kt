package com.nearpick.app.controller

import com.nearpick.domain.transaction.ReservationService
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.transaction.dto.ReservationStatusResponse
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var reservationService: ReservationService

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

    private fun merchantAuth() = authentication(
        UsernamePasswordAuthenticationToken(2L, null, listOf(SimpleGrantedAuthority("ROLE_MERCHANT")))
    )

    private fun pendingResponse() = ReservationStatusResponse(
        reservationId = 1L, status = ReservationStatus.PENDING,
    )

    @Test
    fun `POST api-reservations - CONSUMER 권한으로 201을 반환한다`() {
        whenever(reservationService.create(any(), any(), any())).thenReturn(pendingResponse())

        mockMvc.post("/api/reservations") {
            with(consumerAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":10,"quantity":1}"""
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `PATCH api-reservations-id-cancel - CONSUMER 권한으로 200을 반환한다`() {
        whenever(reservationService.cancel(any(), any())).thenReturn(
            pendingResponse().copy(status = ReservationStatus.CANCELLED)
        )

        mockMvc.patch("/api/reservations/1/cancel") {
            with(consumerAuth())
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `PATCH api-reservations-id-confirm - MERCHANT 권한으로 200을 반환한다`() {
        whenever(reservationService.confirm(any(), any())).thenReturn(
            pendingResponse().copy(status = ReservationStatus.CONFIRMED)
        )

        mockMvc.patch("/api/reservations/1/confirm") {
            with(merchantAuth())
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `GET api-reservations-me - CONSUMER 권한으로 200을 반환한다`() {
        whenever(reservationService.getMyReservations(any(), any(), any())).thenReturn(PageImpl(emptyList()))

        mockMvc.get("/api/reservations/me") {
            with(consumerAuth())
        }.andExpect {
            status { isOk() }
        }
    }
}
