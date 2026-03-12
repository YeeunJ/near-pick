package com.nearpick.nearpick.product.repository

import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
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
                p.shop_lng                                                  AS shopLng,
                p.category                                                  AS category
            FROM products p
            LEFT JOIN popularity_scores ps ON ps.product_id = p.id
            JOIN merchant_profiles mp ON mp.user_id = p.merchant_id
            WHERE p.status = 'ACTIVE'
              AND (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                    COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                        * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                )))) <= :radius
              AND (:category IS NULL OR p.category = :category)
            ORDER BY
                CASE WHEN :sort = 'distance' THEN
                    (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                        COS(RADIANS(:lat)) * COS(RADIANS(p.shop_lat))
                            * COS(RADIANS(p.shop_lng) - RADIANS(:lng))
                        + SIN(RADIANS(:lat)) * SIN(RADIANS(p.shop_lat))
                    )))) END ASC,
                CASE WHEN :sort = 'popularity' THEN COALESCE(ps.score, 0) END DESC,
                p.id ASC
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
              AND (:category IS NULL OR p.category = :category)
        """,
    )
    fun findNearby(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radius") radius: Double,
        @Param("sort") sort: String,
        @Param("category") category: String?,
        pageable: Pageable,
    ): Page<ProductNearbyProjection>

    fun findAllByMerchant_UserId(merchantId: Long, pageable: Pageable): Page<ProductEntity>

    /** 상품 대시보드 목록용 (소량 예상, 최대 100개 제한) */
    fun findTop100ByMerchant_UserId(merchantId: Long): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE (:status IS NULL OR p.status = :status) AND (:category IS NULL OR p.category = :category) ORDER BY p.createdAt DESC")
    fun findAllByOptionalStatus(@Param("status") status: ProductStatus?, @Param("category") category: ProductCategory?, pageable: Pageable): Page<ProductEntity>

    /** 선착순 구매 재고 차감 시 비관적 락 적용 (레거시 — Redis 원자적 차감으로 대체됨) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): ProductEntity?

    /**
     * Redis 원자적 재고 차감 후 DB 동기화.
     * 비관적 락 불필요 — Redis `addAndGet` 이 이미 원자적으로 재고를 보호.
     * 반환값: 업데이트된 행 수 (0이면 DB-Redis 재고 불일치 → Redis 복원 필요)
     */
    @Transactional
    @Modifying
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    fun decrementStockIfSufficient(@Param("id") id: Long, @Param("quantity") quantity: Int): Int
}
