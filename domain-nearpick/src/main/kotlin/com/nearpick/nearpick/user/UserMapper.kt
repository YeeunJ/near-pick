package com.nearpick.nearpick.user

import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupResponse

object UserMapper {
    fun UserEntity.toSignupResponse() = SignupResponse(userId = id, email = email, role = role)
    fun UserEntity.toLoginResult() = LoginResult(userId = id, email = email, role = role)
}
