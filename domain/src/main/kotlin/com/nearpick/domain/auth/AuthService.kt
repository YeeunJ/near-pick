package com.nearpick.domain.auth

import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.auth.dto.SignupMerchantRequest
import com.nearpick.domain.auth.dto.SignupResponse

interface AuthService {
    fun signupConsumer(request: SignupConsumerRequest): SignupResponse
    fun signupMerchant(request: SignupMerchantRequest): SignupResponse
    fun login(request: LoginRequest): LoginResult
}
