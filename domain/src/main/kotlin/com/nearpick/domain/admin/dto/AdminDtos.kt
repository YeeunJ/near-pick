package com.nearpick.domain.admin.dto

import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.AdminLevel
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import java.time.LocalDateTime

data class UserSummary(
    val userId: Long,
    val email: String,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: LocalDateTime,
)

data class AdminProductItem(
    val productId: Long,
    val title: String,
    val price: Int,
    val merchantId: Long,
    val merchantName: String,
    val status: ProductStatus,
    val createdAt: LocalDateTime,
)

data class AdminProfileResponse(
    val adminId: Long,
    val email: String,
    val adminLevel: AdminLevel,
    val permissions: String,
)
