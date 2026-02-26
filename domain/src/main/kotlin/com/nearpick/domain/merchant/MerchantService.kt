package com.nearpick.domain.merchant

import com.nearpick.domain.merchant.dto.MerchantDashboardResponse
import com.nearpick.domain.merchant.dto.MerchantProfileResponse

interface MerchantService {
    fun getDashboard(merchantId: Long): MerchantDashboardResponse
    fun getProfile(merchantId: Long): MerchantProfileResponse
}
