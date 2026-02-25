package com.nearpick.nearpick.user

import com.nearpick.domain.admin.dto.AdminProfileResponse
import com.nearpick.domain.admin.dto.UserSummary

object AdminMapper {

    fun UserEntity.toUserSummary() = UserSummary(
        userId = id,
        email = email,
        role = role,
        status = status,
        createdAt = createdAt,
    )

    fun AdminProfileEntity.toProfileResponse() = AdminProfileResponse(
        adminId = userId,
        email = user.email,
        adminLevel = adminLevel,
        permissions = permissions,
    )
}
