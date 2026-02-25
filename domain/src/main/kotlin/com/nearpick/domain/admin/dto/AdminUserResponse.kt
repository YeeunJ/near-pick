package com.nearpick.domain.admin.dto

import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import java.time.LocalDateTime

data class AdminUserResponse(
    val id: Long,
    val email: String,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: LocalDateTime,
)

data class AdminUserStatusResponse(
    val id: Long,
    val status: UserStatus,
)
