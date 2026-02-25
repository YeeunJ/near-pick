package com.nearpick.nearpick.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun countByProduct_Id(productId: Long): Long
}
