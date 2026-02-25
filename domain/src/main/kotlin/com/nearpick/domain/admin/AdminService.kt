package com.nearpick.domain.admin

import com.nearpick.domain.admin.dto.AdminProductResponse
import com.nearpick.domain.admin.dto.AdminProductStatusResponse
import com.nearpick.domain.admin.dto.AdminUserResponse
import com.nearpick.domain.admin.dto.AdminUserStatusResponse
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import org.springframework.data.domain.Page

interface AdminService {
    fun getUsers(role: UserRole?, status: UserStatus?, query: String?, page: Int, size: Int): Page<AdminUserResponse>
    fun suspendUser(userId: Long): AdminUserStatusResponse
    fun deleteUser(userId: Long)
    fun getProducts(status: ProductStatus?, page: Int, size: Int): Page<AdminProductResponse>
    fun forceClose(productId: Long): AdminProductStatusResponse
}
