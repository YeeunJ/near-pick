package com.nearpick.nearpick.user.mapper

import com.nearpick.domain.admin.dto.AdminProfileResponse
import com.nearpick.domain.admin.dto.UserSummary
import com.nearpick.nearpick.user.entity.AdminProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity

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
