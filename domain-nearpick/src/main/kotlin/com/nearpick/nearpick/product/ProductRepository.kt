package com.nearpick.nearpick.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductRepository : JpaRepository<ProductEntity, Long> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT
                p.id                                                        AS id,
                p.title                                                     AS title,
                p.price                                                     AS price,
                p.product_type                                              AS productType,
                p.status                                                    AS status,
                COALESCE(ps.score, 0)                                       AS popularityScore,
                (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                    COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                        * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                ))))                                                        AS distanceKm,
                mp.business_name                                            AS merchantName
            FROM products p
            LEFT JOIN popularity_scores ps ON ps.product_id = p.id
            JOIN merchant_profiles mp ON mp.user_id = p.merchant_id
            WHERE p.status = 'ACTIVE'
              AND (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                    COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                        * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                )))) <= :radius
            ORDER BY
                CASE WHEN :sort = 'distance' THEN
                    (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                        COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                            * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                        + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                    )))
                END ASC NULLS LAST,
                CASE WHEN :sort = 'popularity' THEN COALESCE(ps.score, 0) END DESC NULLS LAST
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM products p
            WHERE p.status = 'ACTIVE'
              AND (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                    COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                        * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                )))) <= :radius
        """,
    )
    fun findNearby(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radius") radius: Double,
        @Param("sort") sort: String,
        pageable: Pageable,
    ): Page<ProductNearbyProjection>

    fun findAllByMerchant_UserId(merchantId: Long, pageable: Pageable): Page<ProductEntity>
}
