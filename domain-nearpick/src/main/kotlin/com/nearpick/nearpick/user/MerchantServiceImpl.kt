package com.nearpick.nearpick.user

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.merchant.MerchantService
import com.nearpick.domain.merchant.dto.MerchantDashboardResponse
import com.nearpick.domain.merchant.dto.MerchantProfileResponse
import com.nearpick.nearpick.product.PopularityScoreRepository
import com.nearpick.nearpick.product.ProductMapper.toListItem
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.transaction.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.ReservationRepository
import com.nearpick.nearpick.transaction.WishlistRepository
import com.nearpick.nearpick.user.MerchantMapper.toProfileResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MerchantServiceImpl(
    private val merchantProfileRepository: MerchantProfileRepository,
    private val productRepository: ProductRepository,
    private val popularityScoreRepository: PopularityScoreRepository,
    private val reservationRepository: ReservationRepository,
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val wishlistRepository: WishlistRepository,
) : MerchantService {

    override fun getDashboard(merchantId: Long): MerchantDashboardResponse {
        val merchant = merchantProfileRepository.findById(merchantId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }

        val products = productRepository.findTop100ByMerchant_UserId(merchantId)

        val productIds = products.map { it.id }
        val wishlistCounts = if (productIds.isEmpty()) emptyMap()
        else wishlistRepository.countByProductIds(productIds).associate { it.productId to it.cnt }

        val now = LocalDateTime.now()
        val monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay()

        return MerchantDashboardResponse(
            merchantId = merchantId,
            businessName = merchant.businessName,
            totalPopularityScore = popularityScoreRepository.sumScoreByMerchantId(merchantId),
            thisMonthReservationCount = reservationRepository.countByMerchantIdAndPeriod(
                merchantId = merchantId, from = monthStart, to = now,
            ),
            thisMonthPurchaseCount = flashPurchaseRepository.countByMerchantIdAndPeriod(
                merchantId = merchantId, from = monthStart, to = now,
            ),
            products = products.map { product ->
                product.toListItem(wishlistCounts[product.id] ?: 0L)
            },
        )
    }

    override fun getProfile(merchantId: Long): MerchantProfileResponse {
        val merchant = merchantProfileRepository.findById(merchantId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        return merchant.toProfileResponse()
    }
}
