package com.nearpick.domain.merchant

import com.nearpick.domain.merchant.dto.DashboardResponse

interface MerchantService {
    fun getDashboard(merchantId: Long): DashboardResponse
}
