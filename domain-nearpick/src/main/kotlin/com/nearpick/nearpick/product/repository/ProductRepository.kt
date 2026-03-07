package com.nearpick.nearpick.product.repository

import com.nearpick.domain.product.ProductStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import com.nearpick.nearpick.product.entity.ProductEntity

interface ProductRepository : JpaRepository<ProductEntity, Long> {

    /**
     * Haversine 공식을 사용한 반경 내 상품 조회.
     * LEAST/GREATEST 클램핑으로 ACOS 도메인 오류 방지.
     * MySQL 8.x 호환 네이티브 쿼리.
     */
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
                mp.business_name                                            AS merchantName,
                mp.shop_address                                             AS shopAddress,
                p.shop_lat                                                  AS shopLat,
                p.shop_lng                                                  AS shopLng
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
                    )))) END ASC,
                CASE WHEN :sort = 'popularity' THEN COALESCE(ps.score, 0) END DESC
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

    /** 상품 대시보드 목록용 (소량 예상, 최대 100개 제한) */
    fun findTop100ByMerchant_UserId(merchantId: Long): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE :status IS NULL OR p.status = :status ORDER BY p.createdAt DESC")
    fun findAllByOptionalStatus(@Param("status") status: ProductStatus?, pageable: Pageable): Page<ProductEntity>

    /** 선착순 구매 재고 차감 시 비관적 락 적용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): ProductEntity?
}
