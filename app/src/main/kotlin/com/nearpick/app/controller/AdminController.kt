package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.admin.AdminService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin", description = "관리자 전용 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(private val adminService: AdminService) {

    @Operation(summary = "사용자 목록 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/users")
    fun getUsers(
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) status: UserStatus?,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(adminService.getUsers(role, status, query, page, size))

    @Operation(summary = "사용자 정지 처리")
    @SwaggerApiResponse(responseCode = "200", description = "정지 성공")
    @PatchMapping("/users/{userId}/suspend")
    fun suspendUser(@PathVariable userId: Long) =
        ApiResponse.success(adminService.suspendUser(userId))

    @Operation(summary = "사용자 탈퇴 처리")
    @SwaggerApiResponse(responseCode = "204", description = "탈퇴 처리 성공")
    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdrawUser(@PathVariable userId: Long) {
        adminService.withdrawUser(userId)
    }

    @Operation(summary = "상품 목록 조회 (관리자)")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) status: ProductStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(adminService.getProducts(status, page, size))

    @Operation(summary = "상품 강제 마감")
    @SwaggerApiResponse(responseCode = "200", description = "강제 마감 성공")
    @PatchMapping("/products/{productId}/force-close")
    fun forceCloseProduct(@PathVariable productId: Long) =
        ApiResponse.success(adminService.forceCloseProduct(productId))

    @Operation(summary = "관리자 프로필 조회")
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/profile")
    fun getProfile(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(adminService.getProfile(userId))
}
