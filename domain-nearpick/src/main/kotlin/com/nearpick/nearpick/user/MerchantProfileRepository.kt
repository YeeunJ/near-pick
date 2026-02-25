package com.nearpick.nearpick.user

import org.springframework.data.jpa.repository.JpaRepository

interface MerchantProfileRepository : JpaRepository<MerchantProfileEntity, Long> {
    fun existsByBusinessRegNo(businessRegNo: String): Boolean
}
