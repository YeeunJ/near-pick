package com.nearpick.nearpick.user

import com.nearpick.domain.merchant.dto.MerchantProfileResponse

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
