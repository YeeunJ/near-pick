package com.nearpick.nearpick.user.mapper

import com.nearpick.domain.merchant.dto.MerchantProfileResponse
import com.nearpick.nearpick.user.entity.MerchantProfileEntity

object MerchantMapper {

    fun MerchantProfileEntity.toProfileResponse() = MerchantProfileResponse(
        merchantId = userId,
        email = user.email,
        businessName = businessName,
        businessRegNo = businessRegNo,
        shopLat = shopLat,
        shopLng = shopLng,
        shopAddress = shopAddress,
        rating = rating,
        isVerified = isVerified,
    )
}
