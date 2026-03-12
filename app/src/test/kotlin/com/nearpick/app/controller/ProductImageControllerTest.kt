package com.nearpick.app.controller

import com.nearpick.domain.product.ProductImageService
import com.nearpick.domain.product.dto.PresignedUrlResponse
import com.nearpick.domain.product.dto.ProductImageResponse
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
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProductImageControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean lateinit var productImageService: ProductImageService

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
    fun `POST presigned - MERCHANT 권한으로 Presigned URL을 발급한다`() {
        whenever(productImageService.generatePresignedUrl(any(), any(), any())).thenReturn(
            PresignedUrlResponse(
                presignedUrl = "https://presigned.url",
                s3Key = "products/1/images/uuid.jpg",
                expiresInSeconds = 300,
            )
        )

        mockMvc.post("/api/products/1/images/presigned") {
            with(merchantAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """{"filename":"photo.jpg","contentType":"image/jpeg"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.presignedUrl") { value("https://presigned.url") }
            jsonPath("$.data.expiresInSeconds") { value(300) }
        }
    }

    @Test
    fun `POST presigned - 인증 없으면 403을 반환한다`() {
        mockMvc.post("/api/products/1/images/presigned") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"filename":"photo.jpg","contentType":"image/jpeg"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST images - MERCHANT 권한으로 이미지 URL을 저장하면 201을 반환한다`() {
        whenever(productImageService.saveImageUrl(any(), any(), any())).thenReturn(
            ProductImageResponse(id = 10L, url = "https://...", s3Key = "products/1/images/abc.jpg", displayOrder = 0)
        )

        mockMvc.post("/api/products/1/images") {
            with(merchantAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """{"s3Key":"products/1/images/abc.jpg","displayOrder":0}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.id") { value(10) }
        }
    }

    @Test
    fun `DELETE image - MERCHANT 권한으로 이미지를 삭제하면 204를 반환한다`() {
        mockMvc.delete("/api/products/1/images/10") {
            with(merchantAuth())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `PUT order - MERCHANT 권한으로 이미지 순서를 변경하면 200을 반환한다`() {
        whenever(productImageService.reorderImages(any(), any(), any())).thenReturn(
            listOf(ProductImageResponse(id = 1L, url = "u1", s3Key = "k1", displayOrder = 0))
        )

        mockMvc.put("/api/products/1/images/order") {
            with(merchantAuth())
            contentType = MediaType.APPLICATION_JSON
            content = """[{"imageId":1,"displayOrder":0}]"""
        }.andExpect {
            status { isOk() }
        }
    }
}
