package com.nearpick.nearpick.auth

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.auth.AuthService
import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.LoginResult
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.auth.dto.SignupMerchantRequest
import com.nearpick.domain.auth.dto.SignupResponse
import com.nearpick.domain.model.BusinessRegNo
import com.nearpick.domain.model.Email
import com.nearpick.domain.model.Location
import com.nearpick.domain.model.Password
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import com.nearpick.nearpick.user.ConsumerProfileEntity
import com.nearpick.nearpick.user.ConsumerProfileRepository
import com.nearpick.nearpick.user.MerchantProfileEntity
import com.nearpick.nearpick.user.MerchantProfileRepository
import com.nearpick.nearpick.user.UserEntity
import com.nearpick.nearpick.user.UserMapper.toLoginResult
import com.nearpick.nearpick.user.UserMapper.toSignupResponse
import com.nearpick.nearpick.user.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val consumerProfileRepository: ConsumerProfileRepository,
    private val merchantProfileRepository: MerchantProfileRepository,
    private val passwordEncoder: PasswordEncoder,
) : AuthService {

    @Transactional
    override fun signupConsumer(request: SignupConsumerRequest): SignupResponse {
        // Value Object 생성 — 도메인 규칙 검증
        val email = Email(request.email)
        val password = Password(request.password)

        if (userRepository.existsByEmail(email.value)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        val encoded = requireNotNull(passwordEncoder.encode(password.value)) {
            "Password encoding returned null"
        }
        return try {
            val user = userRepository.save(
                UserEntity(email = email.value, passwordHash = encoded, role = UserRole.CONSUMER),
            )
            consumerProfileRepository.save(
                ConsumerProfileEntity(user = user, nickname = email.localPart()),
            )
            user.toSignupResponse()
        } catch (e: DataIntegrityViolationException) {
            // existsByEmail 이후 동시 요청으로 unique 제약 위반 시 재시도 없이 명확한 오류 반환
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    @Transactional
    override fun signupMerchant(request: SignupMerchantRequest): SignupResponse {
        val email = Email(request.email)
        val password = Password(request.password)
        val businessRegNo = BusinessRegNo(request.businessRegNo)
        val location = Location(request.shopLat, request.shopLng)

        if (userRepository.existsByEmail(email.value)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        if (merchantProfileRepository.existsByBusinessRegNo(businessRegNo.value)) {
            throw BusinessException(ErrorCode.DUPLICATE_BUSINESS_REG_NO)
        }
        val encoded = requireNotNull(passwordEncoder.encode(password.value)) {
            "Password encoding returned null"
        }
        return try {
            val user = userRepository.save(
                UserEntity(email = email.value, passwordHash = encoded, role = UserRole.MERCHANT),
            )
            merchantProfileRepository.save(
                MerchantProfileEntity(
                    user = user,
                    businessName = request.businessName,
                    businessRegNo = businessRegNo.value,
                    shopLat = location.lat,
                    shopLng = location.lng,
                    shopAddress = request.shopAddress,
                ),
            )
            user.toSignupResponse()
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    @Transactional(readOnly = true)
    override fun login(request: LoginRequest): LoginResult {
        val user = userRepository.findByEmail(request.email)
            ?: throw BusinessException(ErrorCode.INVALID_CREDENTIALS)

        // 정지/탈퇴 사용자 차단
        when (user.status) {
            UserStatus.SUSPENDED -> throw BusinessException(ErrorCode.ACCOUNT_SUSPENDED)
            UserStatus.WITHDRAWN -> throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
            UserStatus.ACTIVE -> Unit
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }
        return user.toLoginResult()
    }
}
