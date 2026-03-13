# Design: phase12-purchase-lifecycle

> 구매 라이프사이클 완성 — 상태 플로우 · 재고 정책 · 상품 고도화 · 스케줄러

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase12-purchase-lifecycle |
| **Phase** | 12 |
| **목표** | Reservation·FlashPurchase 상태 플로우 완성, 상품 PAUSED/재고 고도화, 재고 복원 정책, 일관성 스케줄러 |
| **작성일** | 2026-03-13 |
| **브랜치** | `feature/phase12-purchase-lifecycle` |
| **참조 Plan** | `docs/01-plan/features/phase12-purchase-lifecycle.plan.md` |

---

## 1. 상태 다이어그램

### 1.1 Reservation 상태 전환

```
                    [소비자 취소]
PENDING ────────────────────────────→ CANCELLED
   │                                    ↑
   │ [소상공인 확정]       [스케줄러: PENDING + visitScheduledAt 초과]
   ↓                                    │
CONFIRMED ──────────────────────────────┘
   │
   ├──[소상공인 취소]──────────────────→ CANCELLED
   │
   ├──[스케줄러: CONFIRMED + visitScheduledAt+2h 초과]──→ NO_SHOW
   │
   └──[방문 코드 입력 (소상공인)]──→ VISITED ──[즉시 자동]──→ COMPLETED
```

### 1.2 FlashPurchase 상태 전환

```
PENDING ──[Kafka Consumer]──→ CONFIRMED ──[픽업 코드 입력 (소상공인)]──→ PICKED_UP
                                  │
                             [소상공인 취소]──→ CANCELLED (재고 복원)
PENDING ──[Kafka 실패]──→ FAILED  (기존 유지)
```

### 1.3 ProductStatus 전환

```
DRAFT ──[create]──→ ACTIVE
ACTIVE ──[소상공인 pause]──→ PAUSED ──[소상공인 resume]──→ ACTIVE
ACTIVE ──[stock=0 자동]──→ PAUSED
PAUSED ──[stock 복원으로 stock>0]──→ ACTIVE (자동)
ACTIVE ──[소상공인 close]──→ CLOSED
ACTIVE ──[관리자 forceClose]──→ FORCE_CLOSED
CLOSED / FORCE_CLOSED → (재활성화 불가)
ACTIVE ──[스케줄러: availableUntil 만료]──→ PAUSED
```

---

## 2. Enum 변경

### 2.1 ReservationStatus 추가

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/ReservationStatus.kt
enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    VISITED,
    COMPLETED,   // 신규: VISITED 후 즉시 전환
    NO_SHOW,     // 신규: 스케줄러 자동 처리
}
```

### 2.2 FlashPurchaseStatus 추가

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/FlashPurchaseStatus.kt
enum class FlashPurchaseStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,   // 신규: 소상공인 취소
    PICKED_UP,   // 신규: 픽업 확인 완료
    COMPLETED,   // 기존 유지 (Kafka 처리 성공 레거시)
    FAILED,
}
```

---

## 3. ErrorCode 추가

```kotlin
// common/src/main/kotlin/com/nearpick/common/exception/ErrorCode.kt

// Product
PRODUCT_NOT_AVAILABLE_YET(422, "아직 판매 시작 전인 상품입니다."),
PRODUCT_AVAILABILITY_EXPIRED(422, "판매 기간이 종료된 상품입니다."),
PRODUCT_FORCE_CLOSED(403, "관리자에 의해 강제 종료된 상품입니다."),
PRODUCT_CANNOT_BE_RESUMED(422, "일시정지 상태인 상품만 재개할 수 있습니다."),
PRODUCT_CANNOT_BE_PAUSED(422, "활성 상태인 상품만 일시정지할 수 있습니다."),

// Reservation
RESERVATION_VISIT_CODE_INVALID(404, "유효하지 않은 방문 코드입니다."),
RESERVATION_ALREADY_COMPLETED(422, "이미 완료된 예약입니다."),

// FlashPurchase
FLASH_PURCHASE_CANNOT_BE_CANCELLED(422, "취소할 수 없는 선착순 구매입니다."),
FLASH_PURCHASE_PICKUP_CODE_INVALID(404, "유효하지 않은 픽업 코드입니다."),
```

---

## 4. Entity 변경

### 4.1 ReservationEntity 필드 추가

```kotlin
// domain-nearpick/.../transaction/entity/ReservationEntity.kt

@Column(length = 6)
var visitCode: String? = null       // confirm 시 생성, 6자리 영숫자

@Column
var completedAt: LocalDateTime? = null   // COMPLETED 전환 시점
```

### 4.2 FlashPurchaseEntity 필드 추가

```kotlin
// domain-nearpick/.../transaction/entity/FlashPurchaseEntity.kt

@Column(length = 6)
var pickupCode: String? = null      // CONFIRMED 시 생성, 6자리 영숫자

@Column
var pickedUpAt: LocalDateTime? = null    // PICKED_UP 전환 시점
```

---

## 5. Flyway V6 마이그레이션

```sql
-- app/src/main/resources/db/migration/V6__purchase_lifecycle.sql

-- reservations 테이블
ALTER TABLE reservations
    ADD COLUMN visit_code   VARCHAR(6)  NULL COMMENT '방문 확인 코드 (confirm 시 생성)',
    ADD COLUMN completed_at DATETIME    NULL COMMENT '방문 완료 시각';

-- flash_purchases 테이블
ALTER TABLE flash_purchases
    ADD COLUMN pickup_code  VARCHAR(6)  NULL COMMENT '픽업 확인 코드 (CONFIRMED 시 생성)',
    ADD COLUMN picked_up_at DATETIME    NULL COMMENT '픽업 완료 시각';

-- 인덱스: 방문 코드 조회용 (소상공인 코드 입력 → 예약 검색)
CREATE UNIQUE INDEX idx_reservations_visit_code
    ON reservations (visit_code)
    WHERE visit_code IS NOT NULL;   -- MySQL: partial index 미지원 → 아래 일반 인덱스 사용

-- MySQL 호환
CREATE INDEX idx_reservations_visit_code ON reservations (visit_code);
CREATE INDEX idx_flash_purchases_pickup_code ON flash_purchases (pickup_code);

-- 스케줄러용 복합 인덱스
CREATE INDEX idx_reservations_status_scheduled
    ON reservations (status, visit_scheduled_at);
```

> **Note**: MySQL은 partial index 미지원. `visit_code` NULL 포함 일반 인덱스 사용.

---

## 6. Repository 변경

### 6.1 ReservationRepository

```kotlin
// domain-nearpick/.../transaction/repository/ReservationRepository.kt

// 방문 코드로 예약 조회 (소상공인 방문 확인)
fun findByVisitCode(visitCode: String): ReservationEntity?

// 소상공인 예약 목록 - 상태 필터 추가
@Query("""
    SELECT r FROM ReservationEntity r
    WHERE r.product.merchant.userId = :merchantId
    AND (:status IS NULL OR r.status = :status)
    ORDER BY r.reservedAt DESC
""")
fun findByMerchantIdAndOptionalStatus(
    @Param("merchantId") merchantId: Long,
    @Param("status") status: ReservationStatus?,
    pageable: Pageable
): Page<ReservationEntity>

// 스케줄러: NO_SHOW 대상 (CONFIRMED + visitScheduledAt+2h 초과)
@Query("""
    SELECT r FROM ReservationEntity r
    WHERE r.status = 'CONFIRMED'
    AND r.visitScheduledAt IS NOT NULL
    AND r.visitScheduledAt < :threshold
""")
fun findConfirmedExpiredForNoShow(
    @Param("threshold") threshold: LocalDateTime
): List<ReservationEntity>

// 스케줄러: 만료 PENDING 대상 (PENDING + visitScheduledAt 초과)
@Query("""
    SELECT r FROM ReservationEntity r
    WHERE r.status = 'PENDING'
    AND r.visitScheduledAt IS NOT NULL
    AND r.visitScheduledAt < :now
""")
fun findPendingExpired(
    @Param("now") now: LocalDateTime
): List<ReservationEntity>
```

### 6.2 FlashPurchaseRepository

```kotlin
// domain-nearpick/.../transaction/repository/FlashPurchaseRepository.kt

// 픽업 코드로 구매 조회
fun findByPickupCode(pickupCode: String): FlashPurchaseEntity?

// 소상공인 구매 목록
@Query("""
    SELECT f FROM FlashPurchaseEntity f
    WHERE f.product.merchant.userId = :merchantId
    AND (:status IS NULL OR f.status = :status)
    ORDER BY f.purchasedAt DESC
""")
fun findByMerchantIdAndOptionalStatus(
    @Param("merchantId") merchantId: Long,
    @Param("status") status: FlashPurchaseStatus?,
    pageable: Pageable
): Page<FlashPurchaseEntity>
```

### 6.3 ProductRepository 추가

```kotlin
// domain-nearpick/.../product/repository/ProductRepository.kt

// stock=0이면 ACTIVE → PAUSED 자동 전환
@Modifying
@Query("""
    UPDATE ProductEntity p
    SET p.status = 'PAUSED'
    WHERE p.id = :id AND p.stock = 0 AND p.status = 'ACTIVE'
""")
fun pauseIfSoldOut(@Param("id") id: Long): Int

// 재고 복원 후 PAUSED → ACTIVE 자동 복원
@Modifying
@Query("""
    UPDATE ProductEntity p
    SET p.status = 'ACTIVE'
    WHERE p.id = :id AND p.stock > 0 AND p.status = 'PAUSED'
""")
fun resumeIfRestored(@Param("id") id: Long): Int

// 재고 증가 (addStock, 취소 시 복원)
@Modifying
@Query("UPDATE ProductEntity p SET p.stock = p.stock + :quantity WHERE p.id = :id")
fun incrementStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

// 스케줄러: availableUntil 만료 → PAUSED
@Modifying
@Query("""
    UPDATE ProductEntity p
    SET p.status = 'PAUSED'
    WHERE p.status = 'ACTIVE'
    AND p.availableUntil IS NOT NULL
    AND p.availableUntil < :now
""")
fun pauseExpiredProducts(@Param("now") now: LocalDateTime): Int
```

---

## 7. 도메인 서비스 인터페이스 변경

### 7.1 ProductService 인터페이스

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/product/ProductService.kt (추가)

fun pauseProduct(merchantId: Long, productId: Long): ProductStatusResponse
fun resumeProduct(merchantId: Long, productId: Long): ProductStatusResponse
fun addStock(merchantId: Long, productId: Long, additionalStock: Int): ProductStatusResponse
```

### 7.2 ReservationService 인터페이스

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/ReservationService.kt (추가)

// 소상공인 취소 (CONFIRMED → CANCELLED)
fun cancelByMerchant(merchantId: Long, reservationId: Long): ReservationStatusResponse

// 방문 코드 입력 (CONFIRMED → VISITED → COMPLETED)
fun visitByCode(merchantId: Long, request: ReservationVisitRequest): ReservationStatusResponse

// 예약 상세 조회 (visitCode 포함)
fun getDetail(userId: Long, reservationId: Long): ReservationDetailResponse

// 소상공인 목록 (상태 필터)
fun getMerchantReservations(
    merchantId: Long,
    status: ReservationStatus?,
    page: Int,
    size: Int
): Page<ReservationItem>
```

### 7.3 FlashPurchaseService 인터페이스

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/FlashPurchaseService.kt (추가)

// 픽업 코드 입력 (CONFIRMED → PICKED_UP)
fun pickupByCode(merchantId: Long, request: FlashPurchasePickupRequest): FlashPurchaseStatusResponse

// 소상공인 취소 (CONFIRMED → CANCELLED + 재고 복원)
fun cancelByMerchant(merchantId: Long, purchaseId: Long): FlashPurchaseStatusResponse

// 구매 상세 조회 (pickupCode 포함)
fun getDetail(userId: Long, purchaseId: Long): FlashPurchaseDetailResponse

// 소상공인 목록 (상태 필터)
fun getMerchantPurchases(
    merchantId: Long,
    status: FlashPurchaseStatus?,
    page: Int,
    size: Int
): Page<FlashPurchaseItem>
```

---

## 8. DTO 설계

### 8.1 신규 Request DTO

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/dto/

data class ReservationVisitRequest(
    @field:NotBlank @field:Size(min = 6, max = 6)
    val code: String,
)

data class FlashPurchasePickupRequest(
    @field:NotBlank @field:Size(min = 6, max = 6)
    val code: String,
)

data class ProductAddStockRequest(
    @field:Positive @field:Max(9999)
    val additionalStock: Int,
)
```

### 8.2 신규 Response DTO

```kotlin
data class ReservationDetailResponse(
    val reservationId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: ReservationStatus,
    val memo: String?,
    val visitScheduledAt: LocalDateTime?,
    val reservedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val visitCode: String?,          // CONSUMER 본인만 조회 가능
)

data class FlashPurchaseDetailResponse(
    val purchaseId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val status: FlashPurchaseStatus,
    val purchasedAt: LocalDateTime,
    val pickedUpAt: LocalDateTime?,
    val pickupCode: String?,         // CONSUMER 본인만 조회 가능
)
```

---

## 9. 서비스 구현 설계

### 9.1 ProductServiceImpl — pause / resume / addStock

```kotlin
@Transactional
override fun pauseProduct(merchantId: Long, productId: Long): ProductStatusResponse {
    val product = productRepository.findById(productId).orElseThrow { ... PRODUCT_NOT_FOUND }
    if (product.merchant.userId != merchantId) throw BusinessException(FORBIDDEN)
    if (product.status != ProductStatus.ACTIVE) throw BusinessException(PRODUCT_CANNOT_BE_PAUSED)
    product.status = ProductStatus.PAUSED
    return product.toStatusResponse()
}

@Transactional
override fun resumeProduct(merchantId: Long, productId: Long): ProductStatusResponse {
    val product = productRepository.findById(productId).orElseThrow { ... }
    if (product.merchant.userId != merchantId) throw BusinessException(FORBIDDEN)
    if (product.status != ProductStatus.PAUSED) throw BusinessException(PRODUCT_CANNOT_BE_RESUMED)
    product.status = ProductStatus.ACTIVE
    return product.toStatusResponse()
}

@Transactional
override fun addStock(merchantId: Long, productId: Long, additionalStock: Int): ProductStatusResponse {
    val product = productRepository.findById(productId).orElseThrow { ... }
    if (product.merchant.userId != merchantId) throw BusinessException(FORBIDDEN)
    if (product.status == ProductStatus.CLOSED || product.status == ProductStatus.FORCE_CLOSED)
        throw BusinessException(FORBIDDEN)
    productRepository.incrementStock(productId, additionalStock)
    // PAUSED(재고소진) 상태면 ACTIVE 자동 복원
    productRepository.resumeIfRestored(productId)
    return productRepository.findById(productId).get().toStatusResponse()
}
```

### 9.2 ProductServiceImpl — availableFrom/Until 검증

```kotlin
// create() 및 getNearby() 에서 호출되는 private 검증 함수
private fun validateAvailability(product: ProductEntity) {
    val now = LocalDateTime.now()
    if (product.availableFrom != null && now.isBefore(product.availableFrom))
        throw BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE_YET)
    if (product.availableUntil != null && now.isAfter(product.availableUntil))
        throw BusinessException(ErrorCode.PRODUCT_AVAILABILITY_EXPIRED)
}
```

### 9.3 AdminServiceImpl — FORCE_CLOSED 버그 수정

```kotlin
// 기존: product.status = ProductStatus.CLOSED
// 수정:
product.status = ProductStatus.FORCE_CLOSED
```

### 9.4 ReservationServiceImpl — create() 재고 감소 추가

```kotlin
@Transactional
override fun create(...): ReservationStatusResponse {
    val product = productRepository.findById(productId).orElseThrow { ... }
    if (product.status != ProductStatus.ACTIVE) throw BusinessException(PRODUCT_NOT_ACTIVE)
    validateAvailability(product)  // availableFrom/Until 검증

    // RESERVATION 타입만 재고 감소
    if (product.productType == ProductType.RESERVATION) {
        val updated = productRepository.decrementStockIfSufficient(productId, request.quantity)
        if (updated == 0) throw BusinessException(OUT_OF_STOCK)
        productRepository.pauseIfSoldOut(productId)   // stock=0이면 PAUSED
    }

    val reservation = ReservationEntity(
        user = user, product = product,
        quantity = request.quantity, memo = request.memo,
        visitScheduledAt = request.visitScheduledAt,
        status = ReservationStatus.PENDING,
    )
    return reservationRepository.save(reservation).toStatusResponse()
}
```

### 9.5 ReservationServiceImpl — confirm() visitCode 생성

```kotlin
@Transactional
override fun confirm(merchantId: Long, reservationId: Long): ReservationStatusResponse {
    val reservation = findAndValidateMerchant(merchantId, reservationId)
    if (reservation.status != ReservationStatus.PENDING) throw BusinessException(RESERVATION_CANNOT_BE_CONFIRMED)
    reservation.status = ReservationStatus.CONFIRMED
    reservation.visitCode = generateCode()   // 6자리 영숫자
    return reservationRepository.save(reservation).toStatusResponse()
}

private fun generateCode(): String {
    val chars = ('A'..'Z') + ('0'..'9')
    return (1..6).map { chars.random() }.joinToString("")
}
```

### 9.6 ReservationServiceImpl — visitByCode()

```kotlin
@Transactional
override fun visitByCode(merchantId: Long, request: ReservationVisitRequest): ReservationStatusResponse {
    val reservation = reservationRepository.findByVisitCode(request.code)
        ?: throw BusinessException(RESERVATION_VISIT_CODE_INVALID)
    // 소상공인 소유 확인
    if (reservation.product.merchant.userId != merchantId) throw BusinessException(FORBIDDEN)
    if (reservation.status != ReservationStatus.CONFIRMED) throw BusinessException(RESERVATION_CANNOT_BE_CONFIRMED)

    val now = LocalDateTime.now()
    reservation.status = ReservationStatus.COMPLETED   // VISITED → COMPLETED 즉시
    reservation.completedAt = now
    reservation.visitCode = null   // 코드 사용 후 무효화
    return reservationRepository.save(reservation).toStatusResponse()
}
```

### 9.7 ReservationServiceImpl — cancelByMerchant()

```kotlin
@Transactional
override fun cancelByMerchant(merchantId: Long, reservationId: Long): ReservationStatusResponse {
    val reservation = findAndValidateMerchant(merchantId, reservationId)
    if (reservation.status !in listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))
        throw BusinessException(RESERVATION_CANNOT_BE_CANCELLED)

    reservation.status = ReservationStatus.CANCELLED
    // 재고 복원
    productRepository.incrementStock(reservation.product.id!!, reservation.quantity)
    productRepository.resumeIfRestored(reservation.product.id!!)
    return reservationRepository.save(reservation).toStatusResponse()
}
```

### 9.8 FlashPurchaseConsumer — CONFIRMED 시 pickupCode 생성

```kotlin
// 기존 CONFIRMED 처리 이후 추가
flashPurchase.pickupCode = generateCode()   // 6자리 영숫자
flashPurchaseRepository.save(flashPurchase)

// stock=0이면 상품 PAUSED
productRepository.pauseIfSoldOut(productId)
```

### 9.9 FlashPurchaseServiceImpl — cancelByMerchant()

```kotlin
@Transactional
override fun cancelByMerchant(merchantId: Long, purchaseId: Long): FlashPurchaseStatusResponse {
    val purchase = flashPurchaseRepository.findById(purchaseId).orElseThrow { ... }
    if (purchase.product.merchant.userId != merchantId) throw BusinessException(FORBIDDEN)
    if (purchase.status != FlashPurchaseStatus.CONFIRMED) throw BusinessException(FLASH_PURCHASE_CANNOT_BE_CANCELLED)

    purchase.status = FlashPurchaseStatus.CANCELLED

    // DB 재고 복원
    productRepository.incrementStock(purchase.product.id!!, purchase.quantity)
    productRepository.resumeIfRestored(purchase.product.id!!)

    // Redis 재고 복원
    val stockKey = "stock:flash:${purchase.product.id}"
    redissonClient.getAtomicLong(stockKey).addAndGet(purchase.quantity.toLong())

    return flashPurchaseRepository.save(purchase).toStatusResponse()
}
```

---

## 10. 스케줄러 설계

### 10.1 ReservationScheduler

```kotlin
// domain-nearpick/.../transaction/scheduler/ReservationScheduler.kt
@Component
class ReservationScheduler(
    private val reservationRepository: ReservationRepository,
    private val productRepository: ProductRepository,
) {
    // 매 시간 정각: NO_SHOW 처리
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun processNoShow() {
        val threshold = LocalDateTime.now().minusHours(2)
        val targets = reservationRepository.findConfirmedExpiredForNoShow(threshold)
        targets.forEach { it.status = ReservationStatus.NO_SHOW }
        // 재고 복원 없음 (해당 시간 슬롯 손실)
    }

    // 매 시간 30분: 만료 PENDING 자동 취소
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    fun processExpiredPending() {
        val now = LocalDateTime.now()
        val targets = reservationRepository.findPendingExpired(now)
        targets.forEach { reservation ->
            reservation.status = ReservationStatus.CANCELLED
            productRepository.incrementStock(reservation.product.id!!, reservation.quantity)
            productRepository.resumeIfRestored(reservation.product.id!!)
        }
    }
}
```

### 10.2 ProductScheduler

```kotlin
// domain-nearpick/.../product/scheduler/ProductScheduler.kt
@Component
class ProductScheduler(
    private val productRepository: ProductRepository,
    private val redissonClient: RedissonClient,
) {
    // 매 시간 정각: availableUntil 만료 상품 PAUSED
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun pauseExpiredProducts() {
        val count = productRepository.pauseExpiredProducts(LocalDateTime.now())
        if (count > 0) log.info("Paused $count expired products")
    }

    // 매일 04:15: Redis↔DB 재고 일관성 검사
    @Scheduled(cron = "0 15 4 * * *")
    fun syncRedisStockWithDb() {
        val keys = redissonClient.keys.getKeysByPattern("stock:flash:*")
        keys.forEach { key ->
            val productId = key.removePrefix("stock:flash:").toLongOrNull() ?: return@forEach
            val redisStock = redissonClient.getAtomicLong(key).get()
            val dbStock = productRepository.findById(productId).map { it.stock.toLong() }.orElse(null)
                ?: return@forEach

            if (redisStock != dbStock) {
                log.warn("[StockSync] productId=$productId Redis=$redisStock DB=$dbStock → Reset Redis to DB")
                redissonClient.getAtomicLong(key).set(dbStock)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProductScheduler::class.java)
    }
}
```

> `@EnableScheduling`은 기존 Application 클래스 또는 별도 Config에 추가

---

## 11. Controller / API 설계

### 11.1 ProductController 추가

```kotlin
// app/.../controller/ProductController.kt

@PatchMapping("/{productId}/pause")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "상품 일시정지", description = "ACTIVE → PAUSED")
fun pause(@PathVariable productId: Long, @AuthenticationPrincipal user: UserDetails): ResponseEntity<ApiResponse<ProductStatusResponse>> {
    val merchantId = user.merchantId()
    return ResponseEntity.ok(ApiResponse.success(productService.pauseProduct(merchantId, productId)))
}

@PatchMapping("/{productId}/resume")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "상품 재개", description = "PAUSED → ACTIVE")
fun resume(@PathVariable productId: Long, @AuthenticationPrincipal user: UserDetails): ResponseEntity<ApiResponse<ProductStatusResponse>>

@PatchMapping("/{productId}/stock")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "재고 추가")
fun addStock(
    @PathVariable productId: Long,
    @RequestBody @Valid request: ProductAddStockRequest,
    @AuthenticationPrincipal user: UserDetails,
): ResponseEntity<ApiResponse<ProductStatusResponse>>
```

### 11.2 ReservationController 추가

```kotlin
// app/.../controller/ReservationController.kt

@GetMapping("/{reservationId}")
@PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
@Operation(summary = "예약 상세 조회 (visitCode 포함)")
fun getDetail(@PathVariable reservationId: Long, ...)

@PatchMapping("/visit")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "방문 코드 확인 → 완료 처리")
fun visit(@RequestBody @Valid request: ReservationVisitRequest, ...)

@PatchMapping("/{reservationId}/cancel")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "소상공인 예약 취소 (PENDING/CONFIRMED)")
fun cancelByMerchant(@PathVariable reservationId: Long, ...)

// 기존 GET /merchant → status 파라미터 추가
@GetMapping("/merchant")
@PreAuthorize("hasRole('MERCHANT')")
fun getMerchantReservations(
    @RequestParam(required = false) status: ReservationStatus?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    ...
)
```

### 11.3 FlashPurchaseController 추가

```kotlin
// app/.../controller/FlashPurchaseController.kt

@GetMapping("/{purchaseId}")
@PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
@Operation(summary = "구매 상세 조회 (pickupCode 포함)")
fun getDetail(@PathVariable purchaseId: Long, ...)

@PatchMapping("/pickup")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "픽업 코드 확인 → PICKED_UP")
fun pickup(@RequestBody @Valid request: FlashPurchasePickupRequest, ...)

@PatchMapping("/{purchaseId}/cancel")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "소상공인 구매 취소 + 재고 복원")
fun cancelByMerchant(@PathVariable purchaseId: Long, ...)

@GetMapping("/merchant")
@PreAuthorize("hasRole('MERCHANT')")
@Operation(summary = "소상공인 구매 목록 (상태 필터)")
fun getMerchantPurchases(
    @RequestParam(required = false) status: FlashPurchaseStatus?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    ...
)
```

---

## 12. findNearby 쿼리 변경

```kotlin
// ProductRepository.kt — findNearby Native Query에 추가

// 기존 WHERE 절:
// WHERE p.status = 'ACTIVE'

// 변경 후:
// WHERE p.status = 'ACTIVE'
//   AND (p.available_from IS NULL OR p.available_from <= NOW())
//   AND (p.available_until IS NULL OR p.available_until >= NOW())
//   AND p.stock > 0   -- 재고 있는 상품만 노출
```

---

## 13. 전체 파일 변경 목록

```
common/
  exception/ErrorCode.kt                      ← 에러코드 9개 추가

domain/
  transaction/ReservationStatus.kt            ← COMPLETED, NO_SHOW 추가
  transaction/FlashPurchaseStatus.kt          ← PICKED_UP, CANCELLED 추가
  transaction/ReservationService.kt           ← 4개 메서드 추가
  transaction/FlashPurchaseService.kt         ← 4개 메서드 추가
  transaction/dto/TransactionDtos.kt          ← 신규 Request/Response DTO
  product/ProductService.kt                   ← 3개 메서드 추가
  product/dto/ProductDtos.kt                  ← ProductAddStockRequest 추가

domain-nearpick/
  transaction/entity/ReservationEntity.kt     ← visitCode, completedAt 추가
  transaction/entity/FlashPurchaseEntity.kt   ← pickupCode, pickedUpAt 추가
  transaction/repository/ReservationRepository.kt ← 4개 쿼리 추가
  transaction/repository/FlashPurchaseRepository.kt ← 2개 쿼리 추가
  transaction/service/ReservationServiceImpl.kt ← 5개 메서드 수정/추가
  transaction/service/FlashPurchaseServiceImpl.kt ← 3개 메서드 추가
  transaction/messaging/FlashPurchaseConsumer.kt ← pickupCode 생성, stock=0 PAUSED
  transaction/scheduler/ReservationScheduler.kt ← 신규 (NO_SHOW, 만료PENDING)
  product/repository/ProductRepository.kt     ← 4개 쿼리 추가 + findNearby 수정
  product/service/ProductServiceImpl.kt       ← 3개 메서드 추가, availableFrom/Until
  product/scheduler/ProductScheduler.kt       ← 신규 (만료PAUSED, Redis동기화)

app/
  controller/ProductController.kt             ← pause/resume/stock 3개 엔드포인트
  controller/ReservationController.kt         ← visit/cancelByMerchant/getDetail/merchant 4개
  controller/FlashPurchaseController.kt       ← pickup/cancelByMerchant/getDetail/merchant 4개
  db/migration/V6__purchase_lifecycle.sql     ← 신규

app-nearpick (AdminService):
  user/service/AdminServiceImpl.kt            ← FORCE_CLOSED 버그 수정
```

---

## 14. 테스트 설계

### 단위 테스트 (Mockito)

| 테스트 클래스 | 주요 케이스 |
|-------------|-----------|
| `ProductServiceImplTest` | pauseProduct, resumeProduct, addStock (PAUSED→ACTIVE 자동), availableFrom/Until 검증 |
| `ReservationServiceImplTest` | create(재고감소/OUT_OF_STOCK), confirm(visitCode생성), visitByCode(COMPLETED), cancelByMerchant(재고복원), cancel(소비자) |
| `FlashPurchaseServiceImplTest` | cancelByMerchant(DB+Redis 복원), pickupByCode(PICKED_UP), getMerchantPurchases |
| `ReservationSchedulerTest` | processNoShow(NO_SHOW 전환), processExpiredPending(CANCELLED+재고복원) |
| `ProductSchedulerTest` | pauseExpiredProducts, syncRedisStockWithDb(불일치 감지·재설정) |
| `AdminServiceImplTest` | forceClose → FORCE_CLOSED 확인 |

### 컨트롤러 테스트 (@SpringBootTest)

| 테스트 클래스 | 주요 케이스 |
|-------------|-----------|
| `ProductControllerTest` | PATCH pause/resume/stock (권한: MERCHANT만) |
| `ReservationControllerTest` | PATCH visit, PATCH cancel(merchant), GET detail |
| `FlashPurchaseControllerTest` | PATCH pickup, PATCH cancel(merchant), GET detail, GET merchant |

---

## 15. 구현 순서

1. `ErrorCode.kt` — 에러코드 추가
2. `ReservationStatus.kt`, `FlashPurchaseStatus.kt` — enum 추가
3. `ReservationEntity.kt`, `FlashPurchaseEntity.kt` — 필드 추가
4. `V6__purchase_lifecycle.sql` — Flyway 마이그레이션
5. `ProductRepository.kt` — 쿼리 추가 + findNearby 수정
6. `ReservationRepository.kt`, `FlashPurchaseRepository.kt` — 쿼리 추가
7. `AdminServiceImpl.kt` — FORCE_CLOSED 버그 수정
8. `ProductServiceImpl.kt` — pause/resume/addStock + availableFrom/Until
9. `ReservationServiceImpl.kt` — create(재고), confirm(code), visit, cancelByMerchant, detail, merchantList
10. `FlashPurchaseConsumer.kt` — pickupCode, pauseIfSoldOut
11. `FlashPurchaseServiceImpl.kt` — cancelByMerchant, pickupByCode, detail, merchantList
12. `ReservationScheduler.kt`, `ProductScheduler.kt` — 스케줄러
13. `TransactionDtos.kt`, `ProductDtos.kt` — DTO 추가
14. `ProductController.kt`, `ReservationController.kt`, `FlashPurchaseController.kt` — API
15. 단위 테스트 → 컨트롤러 테스트
