package com.nearpick.nearpick.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PopularityScoreRepository : JpaRepository<PopularityScoreEntity, Long> {

    @Query("""
        SELECT COALESCE(SUM(ps.score), 0)
        FROM PopularityScoreEntity ps
        WHERE ps.product.merchant.userId = :merchantId
    """)
    fun sumScoreByMerchantId(@Param("merchantId") merchantId: Long): Double
}
