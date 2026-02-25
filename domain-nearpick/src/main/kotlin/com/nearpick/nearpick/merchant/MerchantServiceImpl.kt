package com.nearpick.nearpick.merchant

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.merchant.MerchantService
import com.nearpick.domain.merchant.dto.DashboardProductItem
import com.nearpick.domain.merchant.dto.DashboardResponse
import com.nearpick.domain.merchant.dto.PendingReservationItem
import com.nearpick.nearpick.product.PopularityScoreRepository
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.transaction.FlashPurchaseRepository
import com.nearpick.nearpick.transaction.ReservationRepository
import com.nearpick.nearpick.user.MerchantProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MerchantServiceImpl(
    private val merchantProfileRepository: MerchantProfileRepository,
    private val productRepository: ProductRepository,
    private val reservationRepository: ReservationRepository,
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val popularityScoreRepository: PopularityScoreRepository,
) : MerchantService {

    @Transactional(readOnly = true)
    override fun getDashboard(merchantId: Long): DashboardResponse {
        val merchant = merchantProfileRepository.findById(merchantId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay()

        val todayReservationCount = reservationRepository.countTodayByMerchant(merchantId, startOfDay, endOfDay)
        val todayPurchaseCount = flashPurchaseRepository.countTodayByMerchant(merchantId, startOfDay, endOfDay)
        val popularityScore = popularityScoreRepository.sumScoreByMerchantId(merchantId)

        val pendingReservations = reservationRepository.findPendingByMerchant(merchantId).map { r ->
            PendingReservationItem(
                id = r.id,
                consumerMaskedEmail = maskEmail(r.user.email),
                productTitle = r.product.title,
                visitAt = r.visitScheduledAt,
            )
        }

        val myProducts = productRepository.findAllByMerchant_UserId(merchantId).map { p ->
            DashboardProductItem(
                id = p.id,
                title = p.title,
                status = p.status,
                productType = p.productType,
            )
        }

        return DashboardResponse(
            businessName = merchant.businessName,
            todayReservationCount = todayReservationCount,
            todayPurchaseCount = todayPurchaseCount,
            popularityScore = popularityScore,
            pendingReservations = pendingReservations,
            myProducts = myProducts,
        )
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 2) return email
        return email.take(2) + "**" + email.substring(atIndex)
    }
}
