package com.nearpick.nearpick.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.nearpick.nearpick.user.entity.MerchantProfileEntity

interface MerchantProfileRepository : JpaRepository<MerchantProfileEntity, Long> {
    fun existsByBusinessRegNo(businessRegNo: String): Boolean
}
