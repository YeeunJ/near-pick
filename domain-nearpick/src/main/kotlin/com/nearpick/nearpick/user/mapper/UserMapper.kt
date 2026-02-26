package com.nearpick.nearpick.user.mapper

import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupResponse
import com.nearpick.nearpick.user.entity.UserEntity

object UserMapper {
    fun UserEntity.toSignupResponse() = SignupResponse(userId = id, email = email, role = role)
    fun UserEntity.toLoginResult() = LoginResult(userId = id, email = email, role = role)
}
