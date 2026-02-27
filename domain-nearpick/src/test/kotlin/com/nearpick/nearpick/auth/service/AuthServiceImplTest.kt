package com.nearpick.nearpick.auth.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.auth.dto.LoginRequest
import com.nearpick.domain.auth.dto.SignupConsumerRequest
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import com.nearpick.nearpick.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class AuthServiceImplTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var consumerProfileRepository: ConsumerProfileRepository
    @Mock lateinit var merchantProfileRepository: MerchantProfileRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks lateinit var authService: AuthServiceImpl

    // ───── signupConsumer ─────

    @Test
    fun `signupConsumer - 정상 요청 시 SignupResponse를 반환한다`() {
        // given
        val request = SignupConsumerRequest(email = "user@example.com", password = "pass1234")
        val savedUser = UserEntity(id = 1L, email = "user@example.com", passwordHash = "hashed", role = UserRole.CONSUMER)

        whenever(userRepository.existsByEmail("user@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("pass1234")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(consumerProfileRepository.save(any())).thenReturn(
            ConsumerProfileEntity(user = savedUser, nickname = "user")
        )

        // when
        val response = authService.signupConsumer(request)

        // then
        assertNotNull(response)
        assertEquals(UserRole.CONSUMER, response.role)
    }

    @Test
    fun `signupConsumer - 이메일 중복 시 DUPLICATE_EMAIL 예외를 던진다`() {
        // given
        val request = SignupConsumerRequest(email = "dup@example.com", password = "pass1234")
        whenever(userRepository.existsByEmail("dup@example.com")).thenReturn(true)

        // when / then
        val ex = assertThrows<BusinessException> { authService.signupConsumer(request) }
        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.errorCode)
    }

    // ───── login ─────

    @Test
    fun `login - 정상 요청 시 LoginResult를 반환한다`() {
        // given
        val user = UserEntity(id = 1L, email = "user@example.com", passwordHash = "hashed", role = UserRole.CONSUMER)

        whenever(userRepository.findByEmail("user@example.com")).thenReturn(user)
        whenever(passwordEncoder.matches("pass1234", "hashed")).thenReturn(true)

        // when
        val result = authService.login(LoginRequest(email = "user@example.com", password = "pass1234"))

        // then
        assertEquals(1L, result.userId)
        assertEquals(UserRole.CONSUMER, result.role)
    }

    @Test
    fun `login - 존재하지 않는 이메일이면 INVALID_CREDENTIALS 예외를 던진다`() {
        // given
        whenever(userRepository.findByEmail(any())).thenReturn(null)

        // when / then
        val ex = assertThrows<BusinessException> {
            authService.login(LoginRequest(email = "none@example.com", password = "pass1234"))
        }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.errorCode)
    }

    @Test
    fun `login - 비밀번호 불일치 시 INVALID_CREDENTIALS 예외를 던진다`() {
        // given
        val user = UserEntity(email = "user@example.com", passwordHash = "hashed", role = UserRole.CONSUMER)
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(user)
        whenever(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        // when / then
        val ex = assertThrows<BusinessException> {
            authService.login(LoginRequest(email = "user@example.com", password = "wrong"))
        }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.errorCode)
    }

    @Test
    fun `login - 정지된 계정이면 ACCOUNT_SUSPENDED 예외를 던진다`() {
        // given
        val user = UserEntity(
            email = "user@example.com", passwordHash = "hashed",
            role = UserRole.CONSUMER, status = UserStatus.SUSPENDED,
        )
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(user)

        // when / then
        val ex = assertThrows<BusinessException> {
            authService.login(LoginRequest(email = "user@example.com", password = "pass1234"))
        }
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
    }

    @Test
    fun `login - 탈퇴 계정이면 INVALID_CREDENTIALS 예외를 던진다`() {
        // given
        val user = UserEntity(
            email = "user@example.com", passwordHash = "hashed",
            role = UserRole.CONSUMER, status = UserStatus.WITHDRAWN,
        )
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(user)

        // when / then
        val ex = assertThrows<BusinessException> {
            authService.login(LoginRequest(email = "user@example.com", password = "pass1234"))
        }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.errorCode)
    }
}
