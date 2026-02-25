package com.nearpick.domain.admin

import com.nearpick.domain.admin.dto.AdminProductItem
import com.nearpick.domain.admin.dto.AdminProfileResponse
import com.nearpick.domain.admin.dto.UserSummary
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import org.springframework.data.domain.Page

interface AdminService {
    fun getUsers(role: UserRole?, status: UserStatus?, query: String?, page: Int, size: Int): Page<UserSummary>
    fun banUser(userId: Long): UserSummary
    fun getProducts(status: ProductStatus?, page: Int, size: Int): Page<AdminProductItem>
    fun deactivateProduct(productId: Long): AdminProductItem
    fun getProfile(adminId: Long): AdminProfileResponse
}
