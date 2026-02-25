package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.admin.AdminService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(private val adminService: AdminService) {

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) status: UserStatus?,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(adminService.getUsers(role, status, query, page, size))

    @PostMapping("/users/{userId}/ban")
    fun banUser(@PathVariable userId: Long) =
        ApiResponse.success(adminService.banUser(userId))

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) status: ProductStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.success(adminService.getProducts(status, page, size))

    @PostMapping("/products/{productId}/deactivate")
    fun deactivateProduct(@PathVariable productId: Long) =
        ApiResponse.success(adminService.deactivateProduct(productId))

    @GetMapping("/profile")
    fun getProfile(@AuthenticationPrincipal userId: Long) =
        ApiResponse.success(adminService.getProfile(userId))
}
