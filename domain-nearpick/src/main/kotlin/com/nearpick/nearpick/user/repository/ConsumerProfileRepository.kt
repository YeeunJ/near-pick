package com.nearpick.nearpick.user.repository

import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDateTime

interface ConsumerProfileRepository : JpaRepository<ConsumerProfileEntity, Long> {

    @Modifying
    @Query("""
        UPDATE ConsumerProfileEntity c
        SET c.currentLat = :lat, c.currentLng = :lng, c.updatedAt = :now
        WHERE c.userId = :userId
    """)
    fun updateCurrentLocation(
        userId: Long,
        lat: BigDecimal,
        lng: BigDecimal,
        now: LocalDateTime,
    ): Int
}
