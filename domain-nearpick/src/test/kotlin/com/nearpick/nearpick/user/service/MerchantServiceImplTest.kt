package com.nearpick.nearpick.user.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.transaction.ReservationStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.nearpick.product.repository.PopularityScoreRepository
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.transaction.repository.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.repository.ReservationRepository
import com.nearpick.nearpick.transaction.repository.WishlistRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity
import com.nearpick.nearpick.user.entity.UserEntity
import com.nearpick.nearpick.user.repository.MerchantProfileRepository
import org.springframework.data.domain.PageImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class MerchantServiceImplTest {

    @Mock lateinit var merchantProfileRepository: MerchantProfileRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var popularityScoreRepository: PopularityScoreRepository
    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var wishlistRepository: WishlistRepository

    @InjectMocks lateinit var merchantService: MerchantServiceImpl

    private lateinit var merchantUser: UserEntity
    private lateinit var merchantProfile: MerchantProfileEntity

    @BeforeEach
    fun setUp() {
        merchantUser = UserEntity(id = 1L, email = "merchant@example.com", passwordHash = "h", role = UserRole.MERCHANT)
        merchantProfile = MerchantProfileEntity(
            userId = 1L, user = merchantUser, businessName = "내 가게",
            businessRegNo = "123-45-67890",
            shopLat = BigDecimal("37.5"), shopLng = BigDecimal("127.0"),
        )
    }

    // ───── getDashboard ─────

    @Test
    fun `getDashboard - 정상 요청 시 MerchantDashboardResponse를 반환한다`() {
        // given
        whenever(merchantProfileRepository.findById(1L)).thenReturn(Optional.of(merchantProfile))
        whenever(productRepository.findTop100ByMerchant_UserId(1L)).thenReturn(emptyList())
        whenever(popularityScoreRepository.sumScoreByMerchantId(1L)).thenReturn(0.0)
        whenever(reservationRepository.countByMerchantIdAndPeriod(any(), any(), any())).thenReturn(0L)
        whenever(flashPurchaseRepository.countByMerchantIdAndPeriod(any(), any(), any())).thenReturn(0L)
        whenever(reservationRepository.findByMerchantIdAndStatus(any(), any(), any())).thenReturn(PageImpl(emptyList()))

        // when
        val response = merchantService.getDashboard(1L)

        // then
        assertNotNull(response)
        assertEquals(1L, response.merchantId)
        assertEquals("내 가게", response.businessName)
    }

    @Test
    fun `getDashboard - 존재하지 않는 merchantId이면 USER_NOT_FOUND 예외를 던진다`() {
        // given
        whenever(merchantProfileRepository.findById(99L)).thenReturn(Optional.empty())

        // when / then
        val ex = assertThrows<BusinessException> { merchantService.getDashboard(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    // ───── getProfile ─────

    @Test
    fun `getProfile - 정상 요청 시 MerchantProfileResponse를 반환한다`() {
        // given
        whenever(merchantProfileRepository.findById(1L)).thenReturn(Optional.of(merchantProfile))

        // when
        val response = merchantService.getProfile(1L)

        // then
        assertNotNull(response)
        assertEquals("내 가게", response.businessName)
    }

    @Test
    fun `getProfile - 존재하지 않는 merchantId이면 USER_NOT_FOUND 예외를 던진다`() {
        // given
        whenever(merchantProfileRepository.findById(99L)).thenReturn(Optional.empty())

        // when / then
        val ex = assertThrows<BusinessException> { merchantService.getProfile(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }
}
