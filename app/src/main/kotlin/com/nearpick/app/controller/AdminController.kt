package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.admin.AdminService
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService,
) {

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) status: UserStatus?,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ) = ApiResponse.success(adminService.getUsers(role, status, query, page, size))

    @PatchMapping("/users/{id}/suspend")
    fun suspendUser(@PathVariable id: Long) =
        ApiResponse.success(adminService.suspendUser(id))

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: Long) {
        adminService.deleteUser(id)
    }

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) status: ProductStatus?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ) = ApiResponse.success(adminService.getProducts(status, page, size))

    @PatchMapping("/products/{id}/force-close")
    fun forceClose(@PathVariable id: Long) =
        ApiResponse.success(adminService.forceClose(id))
}
