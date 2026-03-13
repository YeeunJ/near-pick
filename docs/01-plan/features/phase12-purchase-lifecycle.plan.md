# Plan: phase12-purchase-lifecycle

## Feature Overview

예약(Reservation)·선착순 구매(FlashPurchase)의 상태 플로우 완성과 함께,
**상품 상태(ProductStatus)·재고(stock) 고도화**, **재고 복원 정책**, **일관성 보장 스케줄러**를 종합적으로 구현한다.
구매 라이프사이클 전체를 end-to-end로 완성하고 플랫폼 신뢰성을 확보하는 것이 목표다.

## Background & 현재 상태 분석

### 현재 구현 현황 (Phase 4~11 기준)

| 영역 | 현재 상태 | 문제점 |
|------|----------|--------|
| `ProductStatus` | DRAFT/ACTIVE/PAUSED/CLOSED/FORCE_CLOSED 정의됨 | PAUSED API 없음, FORCE_CLOSED 버그 (CLOSED로 잘못 설정) |
| stock 관리 | FlashPurchase만 Redis+DB 감소, Reservation 미감소 | Reservation 생성 시 슬롯 점유 없음 → 오버부킹 가능 |
| stock=0 처리 | 상품이 ACTIVE로 계속 노출됨 | 매진 상품이 근처 목록에 표시 |
| `availableFrom/Until` | 저장만 함, 조회·구매 시 미검증 | 판매 기간 외 상품도 검색·구매 가능 |
| Reservation 취소 정책 | 소비자 PENDING만 취소 가능 | 소상공인 CONFIRMED 취소 불가 |
| FlashPurchase 취소 | 없음 | 소상공인이 구매 취소 불가, 재고 복원 없음 |
| 재고 복원 | 없음 | 취소/NO_SHOW 시 재고 증가 로직 없음 |
| 스케줄러 | NO_SHOW 없음, 재고 일관성 없음 | Redis↔DB 불일치 방치, 만료 상품 미처리 |
| AdminService.forceClose | CLOSED로 설정됨 (버그) | FORCE_CLOSED ≠ CLOSED 구분 불가 |

---

## Scope

---

### 기능 1: 상품 상태 고도화

#### 1-1. PAUSED 기능 구현 (소상공인 수동 일시정지)

```
ACTIVE ⇄ PAUSED  (소상공인 수동)
ACTIVE → CLOSED  (소상공인 영구 종료, 기존)
ACTIVE → FORCE_CLOSED  (관리자)
ACTIVE → PAUSED  (스케줄러 자동: availableUntil 만료, stock=0)
```

**신규 API:**
| 메서드 | 엔드포인트 | 역할 | 전환 |
|--------|-----------|------|------|
| PATCH | `/api/products/{id}/pause` | MERCHANT | ACTIVE → PAUSED |
| PATCH | `/api/products/{id}/resume` | MERCHANT | PAUSED → ACTIVE |

**제약:**
- PAUSED 상품: 근처 검색 제외, 예약/구매 불가 (`PRODUCT_NOT_ACTIVE`)
- CLOSED / FORCE_CLOSED 상품은 재활성화 불가

#### 1-2. FORCE_CLOSED 버그 수정

- `AdminServiceImpl.forceCloseProduct()` 에서 `ProductStatus.CLOSED` → `ProductStatus.FORCE_CLOSED`로 수정
- 신규 ErrorCode 추가: `PRODUCT_FORCE_CLOSED(403, "관리자에 의해 강제 종료된 상품입니다.")`

#### 1-3. stock=0 상품 처리

- **FLASH_SALE**: FlashPurchaseConsumer에서 stock이 0이 된 직후 → `status = PAUSED` 자동 전환
  - Redis 재고 = 0이 되는 시점에 `productRepository.updateStatusIfSoldOut()` 호출
- **RESERVATION**: `createReservation()` 이후 stock=0이면 → `status = PAUSED` 자동 전환
- **재활성화**: 소상공인이 `PATCH /api/products/{id}/stock` (재고 추가) + resume 또는 자동 resume
  - stock > 0이 되면 PAUSED(재고소진 이유) → ACTIVE 자동 복원? **별도 API로 분리 (resume 사용)**

#### 1-4. availableFrom/Until 시행

**findNearby 쿼리 추가 조건:**
```sql
AND (p.available_from IS NULL OR p.available_from <= NOW())
AND (p.available_until IS NULL OR p.available_until >= NOW())
```

**예약/구매 시 서비스 레이어 검증 추가:**
```kotlin
if (product.availableFrom != null && now.isBefore(product.availableFrom))
    throw BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE_YET)
if (product.availableUntil != null && now.isAfter(product.availableUntil))
    throw BusinessException(ErrorCode.PRODUCT_AVAILABILITY_EXPIRED)
```

**신규 ErrorCode:**
- `PRODUCT_NOT_AVAILABLE_YET(422, "아직 판매 시작 전인 상품입니다.")`
- `PRODUCT_AVAILABILITY_EXPIRED(422, "판매 기간이 종료된 상품입니다.")`

---

### 기능 2: Reservation 상태 플로우 완성

**목표 플로우:**
```
PENDING ──[소비자 취소]──→ CANCELLED (+재고 복원)
   │
[소상공인 확정]
   ↓
CONFIRMED ──[소상공인 취소]──→ CANCELLED (+재고 복원)
   │            └─[미확정 만료: 스케줄러]──→ CANCELLED (+재고 복원)
[방문 코드 입력]
   ↓
VISITED ──→ COMPLETED (즉시 자동)
   │
   └─[NO_SHOW: 스케줄러]──→ NO_SHOW (재고 복원 없음)
```

**추가 전환:**
- `PENDING` + visitScheduledAt 초과 → `CANCELLED` (스케줄러, 소상공인 미확정 처리)
- `CONFIRMED` + visitScheduledAt+2h 초과 → `NO_SHOW` (스케줄러)
- `CONFIRMED` → `CANCELLED` (소상공인 직접 취소)
- `VISITED` → `COMPLETED` (방문 코드 입력 즉시 자동)

**추가 enum:** `COMPLETED`, `NO_SHOW`

**추가 Entity 필드:**
- `visitCode: String?` — 6자리 영숫자, confirm 시 생성
- `completedAt: LocalDateTime?`

**신규 API:**
| 메서드 | 엔드포인트 | 역할 | 설명 |
|--------|-----------|------|------|
| PATCH | `/api/reservations/visit` | MERCHANT | `{code}` 입력 → VISITED → COMPLETED |
| PATCH | `/api/reservations/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED |
| GET | `/api/reservations/{id}` | CONSUMER | 상세 조회 (visitCode 포함) |
| GET | `/api/reservations/merchant` | MERCHANT | 상태 필터 추가 (`?status=`) |

---

### 기능 3: FlashPurchase 상태 플로우 완성

**목표 플로우:**
```
PENDING ──[Kafka 처리]──→ CONFIRMED ──[픽업 코드 입력]──→ PICKED_UP
                              │
                         [소상공인 취소]──→ CANCELLED (+재고 복원)
FAILED: Kafka 처리 실패 (기존 유지)
```

**추가 enum:** `PICKED_UP`

**추가 Entity 필드:**
- `pickupCode: String?` — 6자리 영숫자, CONFIRMED 시 FlashPurchaseConsumer에서 생성
- `pickedUpAt: LocalDateTime?`

**신규 API:**
| 메서드 | 엔드포인트 | 역할 | 설명 |
|--------|-----------|------|------|
| PATCH | `/api/flash-purchases/pickup` | MERCHANT | `{code}` 입력 → PICKED_UP |
| PATCH | `/api/flash-purchases/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED + 재고 복원 |
| GET | `/api/flash-purchases/{id}` | CONSUMER | 상세 조회 (pickupCode 포함) |
| GET | `/api/flash-purchases/merchant` | MERCHANT | 소상공인 구매 목록 (상태 필터) |

---

### 기능 4: 재고 복원 정책 (Stock Restoration Policy)

| 이벤트 | DB stock | Redis counter | 상품 상태 변경 |
|--------|----------|--------------|--------------|
| Reservation 생성 (PENDING) | -quantity | — | stock=0이면 PAUSED |
| Reservation 취소 (소비자·소상공인) | +quantity | — | PAUSED이면 ACTIVE로 복원 |
| Reservation NO_SHOW (스케줄러) | **복원 없음** | — | 없음 (해당 시간 슬롯 손실) |
| PENDING 만료 자동 취소 (스케줄러) | +quantity | — | PAUSED이면 ACTIVE로 복원 |
| FlashPurchase 구매 | -quantity | Redis -quantity | stock=0이면 PAUSED |
| FlashPurchase 소상공인 취소 | +quantity | Redis +quantity | PAUSED이면 ACTIVE로 복원 |

**PAUSED → ACTIVE 자동 복원 조건:** `stock > 0 AND 취소·복원으로 stock이 0에서 양수가 된 경우`

---

### 기능 5: 스케줄러 (ReservationScheduler + ProductScheduler)

#### 5-1. ReservationScheduler

```kotlin
@Scheduled(cron = "0 0 * * * *")  // 매 시간 정각
fun processNoShow()
// 조건: status=CONFIRMED AND visitScheduledAt IS NOT NULL AND visitScheduledAt + 2h < now
// 처리: status → NO_SHOW (재고 변경 없음)

@Scheduled(cron = "0 30 * * * *")  // 매 시간 30분
fun processExpiredPending()
// 조건: status=PENDING AND visitScheduledAt IS NOT NULL AND visitScheduledAt < now
// 처리: status → CANCELLED, stock +quantity (재고 복원), PAUSED이면 ACTIVE 복원
```

#### 5-2. ProductScheduler

```kotlin
@Scheduled(cron = "0 0 * * * *")  // 매 시간 정각
fun closeExpiredProducts()
// 조건: status=ACTIVE AND availableUntil IS NOT NULL AND availableUntil < now
// 처리: status → PAUSED

@Scheduled(cron = "0 15 4 * * *")  // 매일 04:15
fun syncRedisStockWithDb()
// 대상: Redis "stock:flash:*" 키 존재하는 상품
// 처리: Redis 재고 ≠ DB 재고이면 → 로그 경고 + Redis 값 DB로 초기화
// 기준: DB를 진실의 원천(source of truth)으로 사용
```

---

### 기능 6: 취소 정책 정리 (Summary)

| 역할 | 대상 | 허용 상태 | 재고 복원 |
|------|------|----------|----------|
| 소비자 | Reservation | PENDING | +quantity |
| 소상공인 | Reservation | PENDING, CONFIRMED | +quantity |
| 소상공인 | FlashPurchase | CONFIRMED | +quantity (DB + Redis) |
| 스케줄러 | Reservation (만료 PENDING) | PENDING + visitScheduledAt 초과 | +quantity |
| 소비자 | FlashPurchase | **불가** | — |

---

### 기능 7: Reservation 생성 시 재고 감소 (신규)

현재 예약 생성 시 재고를 감소하지 않아 오버부킹이 가능하다.

- `ReservationServiceImpl.create()` — `ProductRepository.decrementStockIfSufficient()` 호출
  - stock < quantity → `OUT_OF_STOCK`
  - 성공 시 stock=0이면 → `status = PAUSED`
- `RESERVATION` 타입 상품 전용 (FLASH_SALE은 Kafka에서 처리)
- 기존 `decrementStockIfSufficient()` 재사용 (Flash Purchase용과 동일 쿼리)

---

### 기능 8: 상품 재고 수동 보충 API (소상공인)

- `PATCH /api/products/{id}/stock` — `{ additionalStock: Int }` → stock += additionalStock
- 전제 조건: 소상공인 소유 상품, stock > 0이 되면 PAUSED(재고소진) 상태 자동 ACTIVE 복원
  - CLOSED / FORCE_CLOSED는 복원 불가

---

## Out of Scope

- QR 코드 이미지 생성 (텍스트 코드 사용)
- 결제/환불 금액 처리
- 소비자 FCM/앱 알림 (Phase 14)
- 소비자 FlashPurchase 직접 취소
- visitScheduledAt 없는 예약의 NO_SHOW 자동 처리

---

## Implementation Order

1. **Enum 추가** — `ReservationStatus.COMPLETED/NO_SHOW`, `FlashPurchaseStatus.PICKED_UP`
2. **ErrorCode 추가** — PRODUCT_NOT_AVAILABLE_YET, PRODUCT_AVAILABILITY_EXPIRED, PRODUCT_FORCE_CLOSED
3. **Entity 필드 추가** — visitCode, completedAt, pickupCode, pickedUpAt
4. **Flyway V6** — 스키마 변경 마이그레이션
5. **ProductService 확장** — pause/resume/addStock + availableFrom/Until 검증 + findNearby 조건 추가
6. **AdminService 버그 수정** — FORCE_CLOSED 수정
7. **ReservationService 확장** — create(재고감소), visit, cancel(소상공인), 만료 PENDING 처리
8. **FlashPurchaseService 확장** — pickup, cancel(재고복원)
9. **FlashPurchaseConsumer 수정** — CONFIRMED 시 pickupCode 생성, stock=0 시 PAUSED 처리
10. **스케줄러 구현** — ReservationScheduler, ProductScheduler
11. **컨트롤러 추가** — 신규 API 엔드포인트
12. **테스트 작성** — 상태 전환·재고 복원·스케줄러 단위 테스트

---

## Success Criteria

- Reservation 전체 플로우 동작 (PENDING→CONFIRMED→VISITED→COMPLETED / NO_SHOW / CANCELLED)
- FlashPurchase 픽업 코드 시스템 동작 (CONFIRMED→PICKED_UP)
- stock=0이면 상품 PAUSED 자동 전환, 재고 복원 시 ACTIVE 복원
- availableFrom/Until 필터 findNearby 및 예약/구매 검증 적용
- FORCE_CLOSED 올바르게 설정됨
- NO_SHOW 스케줄러·만료 PENDING 스케줄러·Redis↔DB 재고 일관성 스케줄러 단위 테스트 통과
- FlashPurchase 소상공인 취소 시 Redis + DB 재고 +1 확인
- Reservation 생성 시 재고 감소 확인
- 기존 테스트 전부 GREEN, 빌드 통과

---

## New API Summary (전체)

| # | 메서드 | 엔드포인트 | 역할 | 설명 |
|---|--------|-----------|------|------|
| 1 | PATCH | `/api/products/{id}/pause` | MERCHANT | ACTIVE → PAUSED |
| 2 | PATCH | `/api/products/{id}/resume` | MERCHANT | PAUSED → ACTIVE |
| 3 | PATCH | `/api/products/{id}/stock` | MERCHANT | 재고 추가 (+additionalStock) |
| 4 | PATCH | `/api/reservations/visit` | MERCHANT | 방문 코드 → VISITED → COMPLETED |
| 5 | PATCH | `/api/reservations/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED |
| 6 | GET | `/api/reservations/{id}` | CONSUMER | 상세 조회 (visitCode 포함) |
| 7 | GET | `/api/reservations/merchant` | MERCHANT | 상태 필터링 (`?status=`) |
| 8 | PATCH | `/api/flash-purchases/pickup` | MERCHANT | 픽업 코드 → PICKED_UP |
| 9 | PATCH | `/api/flash-purchases/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED + 재고 복원 |
| 10 | GET | `/api/flash-purchases/{id}` | CONSUMER | 상세 조회 (pickupCode 포함) |
| 11 | GET | `/api/flash-purchases/merchant` | MERCHANT | 소상공인 구매 목록 (상태 필터) |

---

## Estimated Effort

| 항목 | 예상 시간 |
|------|---------|
| Enum/ErrorCode/Entity/Flyway | 40분 |
| ProductService 확장 (pause/resume/stock/validations) | 1.5시간 |
| AdminService 버그 수정 | 10분 |
| ReservationService 확장 | 1.5시간 |
| FlashPurchaseService + Consumer 수정 | 1.5시간 |
| 스케줄러 2개 (Reservation + Product) | 1시간 |
| 컨트롤러 (11개 엔드포인트) | 1시간 |
| 테스트 | 2시간 |
| **합계** | **~10시간** |
