package com.nearpick.nearpick.auth

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.auth.AuthService
import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.auth.dto.SignupMerchantRequest
import com.nearpick.domain.auth.dto.SignupResponse
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.user.ConsumerProfileEntity
import com.nearpick.nearpick.user.ConsumerProfileRepository
import com.nearpick.nearpick.user.MerchantProfileEntity
import com.nearpick.nearpick.user.MerchantProfileRepository
import com.nearpick.nearpick.user.UserEntity
import com.nearpick.nearpick.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val consumerProfileRepository: ConsumerProfileRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
    private val passwordEncoder: PasswordEncoder,
) : AuthService {

    override fun signupConsumer(request: SignupConsumerRequest): SignupResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        val user = userRepository.save(
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password)!!,
                role = UserRole.CONSUMER,
            )
        )
        val defaultNickname = request.email.substringBefore("@")
        consumerProfileRepository.save(ConsumerProfileEntity(user = user, nickname = defaultNickname))
        return SignupResponse(userId = user.id, email = user.email, role = user.role)
    }

    override fun signupMerchant(request: SignupMerchantRequest): SignupResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        if (merchantProfileRepository.existsByBusinessRegNo(request.businessRegNo)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        val user = userRepository.save(
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password)!!,
                role = UserRole.MERCHANT,
            )
        )
        merchantProfileRepository.save(
            MerchantProfileEntity(
                user = user,
                businessName = request.businessName,
                businessRegNo = request.businessRegNo,
                shopLat = request.shopLat,
                shopLng = request.shopLng,
                shopAddress = request.shopAddress,
            )
        )
        return SignupResponse(userId = user.id, email = user.email, role = user.role)
    }

    @Transactional(readOnly = true)
    override fun login(request: LoginRequest): LoginResult {
        val user = userRepository.findByEmail(request.email)
            ?: throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }
        return LoginResult(userId = user.id, email = user.email, role = user.role)
    }
}
