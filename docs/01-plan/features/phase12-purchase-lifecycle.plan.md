# Plan: phase12-purchase-lifecycle

## Feature Overview

예약(Reservation)과 선착순 구매(FlashPurchase)의 상태 플로우를 완성한다.
방문/픽업 확인 기능, NO_SHOW 자동 스케줄러, 취소 정책 확장을 구현하여
구매 라이프사이클을 end-to-end로 완성한다.

## Background

- **현재 상태 (Phase 4~9 구현 기준):**
  - Reservation: `create(PENDING)`, `confirm(CONFIRMED)`, `cancel(CANCELLED)` — 3개 전환만 존재
  - FlashPurchase: `purchase(PENDING→CONFIRMED via Kafka)` — cancel/pickup 없음
  - `VISITED`, `COMPLETED`, `NO_SHOW` (Reservation), `PICKED_UP` (FlashPurchase) 미사용
- **목표:** 상태 플로우 완성 + 방문/픽업 코드 시스템 + 자동 스케줄러

## Scope

---

### 기능 1: Reservation 상태 플로우 완성

**목표 플로우:**
```
PENDING → CONFIRMED → VISITED → COMPLETED
                 ↓         ↓
              CANCELLED  NO_SHOW (자동)
소비자 취소: PENDING → CANCELLED
소상공인 취소: PENDING or CONFIRMED → CANCELLED
```

**현재 빠진 전환:**
- `CONFIRMED → VISITED` (소상공인 방문 확인)
- `VISITED → COMPLETED` (자동 또는 소상공인 확정)
- `CONFIRMED → NO_SHOW` (스케줄러 자동)
- `CONFIRMED → CANCELLED` (소상공인 취소 — 현재 소비자 PENDING만 가능)

**추가 항목:**
- `ReservationStatus`에 `COMPLETED`, `NO_SHOW` 추가
- `ReservationEntity`에 `visitCode: String?` 필드 추가 (confirm 시 6자리 코드 생성)
- `ReservationEntity`에 `completedAt: LocalDateTime?` 필드 추가

---

### 기능 2: 방문 확인 코드 시스템 (Reservation)

- `confirm()` 시 6자리 랜덤 코드(영숫자) 생성 → `visitCode` 저장
- 소비자: 예약 상세 조회 시 `visitCode` 반환 (앱에서 소상공인에게 제시)
- 소상공인: `PATCH /api/reservations/visit` + `{ code }` → `VISITED`로 전환
- `VISITED → COMPLETED` 자동 전환 (VISITED 도달 즉시 COMPLETED 처리, 별도 API 불필요)

**API 추가:**
| 메서드 | 엔드포인트 | 역할 | 설명 |
|--------|-----------|------|------|
| PATCH | `/api/reservations/visit` | MERCHANT | 방문 코드 입력 → VISITED + COMPLETED |
| PATCH | `/api/reservations/{id}/cancel` | MERCHANT | CONFIRMED 예약 소상공인 취소 |
| GET | `/api/reservations/{id}` | CONSUMER/MERCHANT | 예약 상세 (visitCode 포함) |

---

### 기능 3: FlashPurchase 상태 플로우 완성

**목표 플로우:**
```
PENDING → CONFIRMED → PICKED_UP
              ↓
           CANCELLED (소상공인)
FAILED: Kafka 처리 실패 (기존 유지)
```

**추가 항목:**
- `FlashPurchaseStatus`에 `PICKED_UP` 추가 (기존 `COMPLETED`는 유지)
- `FlashPurchaseEntity`에 `pickupCode: String?` 필드 추가 (CONFIRMED 시 생성)
- `FlashPurchaseEntity`에 `pickedUpAt: LocalDateTime?` 필드 추가

**API 추가:**
| 메서드 | 엔드포인트 | 역할 | 설명 |
|--------|-----------|------|------|
| PATCH | `/api/flash-purchases/pickup` | MERCHANT | 픽업 코드 입력 → PICKED_UP |
| PATCH | `/api/flash-purchases/{id}/cancel` | MERCHANT | CONFIRMED 구매 취소 (재고 복원) |
| GET | `/api/flash-purchases/{id}` | CONSUMER/MERCHANT | 구매 상세 (pickupCode 포함) |

---

### 기능 4: NO_SHOW 자동 스케줄러

- `@Scheduled(cron = "0 0 * * * *")` — 매 시간 정각 실행
- 조건: `status = CONFIRMED AND visitScheduledAt < now() - 2시간`
- 처리: `status → NO_SHOW`
- 위치: `domain-nearpick/transaction/scheduler/ReservationScheduler.kt`
- `visitScheduledAt`이 null인 예약은 스케줄러 대상 제외

---

### 기능 5: 취소 정책 정리

| 역할 | 대상 | 조건 | 동작 |
|------|------|------|------|
| 소비자 | Reservation | status = PENDING | CANCELLED |
| 소상공인 | Reservation | status = PENDING or CONFIRMED | CANCELLED |
| 소상공인 | FlashPurchase | status = CONFIRMED | CANCELLED + 재고 복원 (+1) |

- FlashPurchase 취소 시 Redis 재고 카운터 + DB stock 복원
- 소비자 FlashPurchase 취소는 **Out of Scope** (CONFIRMED 이후 소비자 직접 취소 불가)

---

### 기능 6: 상태별 조회 API 확장

현재 소상공인은 PENDING 예약만 조회 가능. 확장:
- `GET /api/reservations/merchant?status={status}` — 상태별 필터링 추가
- `GET /api/flash-purchases/merchant?status={status}` — 소상공인 구매 목록 추가 (현재 없음)

---

## Out of Scope

- QR 코드 이미지 생성 (텍스트 코드로 대체)
- 결제/환불 금액 처리 (Payment 시스템 별도)
- 소비자 FCM/앱 알림 (Phase 14)
- 소비자 FlashPurchase 직접 취소
- visitScheduledAt 없는 예약의 NO_SHOW 처리

## Implementation Order

1. **Enum 추가** — `ReservationStatus.COMPLETED`, `NO_SHOW`, `FlashPurchaseStatus.PICKED_UP`
2. **Entity 필드 추가** — `visitCode`, `completedAt`, `pickupCode`, `pickedUpAt`
3. **Flyway V6** — 스키마 변경 마이그레이션
4. **서비스 구현** — visit/pickup/cancel/scheduler
5. **컨트롤러 추가** — 신규 API 엔드포인트
6. **테스트 작성** — 상태 전환 단위 테스트

## Success Criteria

- Reservation 전체 상태 플로우 (PENDING→CONFIRMED→VISITED→COMPLETED, NO_SHOW, CANCELLED) API 동작
- FlashPurchase 픽업 코드 시스템 동작 (CONFIRMED→PICKED_UP)
- 소상공인 방문 코드 입력 → 즉시 COMPLETED 전환
- NO_SHOW 스케줄러 단위 테스트 통과
- FlashPurchase 소상공인 취소 시 재고 복원 확인
- 기존 테스트 전부 GREEN, 빌드 통과

## New API Summary

| # | 메서드 | 엔드포인트 | 역할 | 상태 전환 |
|---|--------|-----------|------|----------|
| 1 | PATCH | `/api/reservations/visit` | MERCHANT | CONFIRMED → VISITED → COMPLETED |
| 2 | PATCH | `/api/reservations/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED |
| 3 | GET | `/api/reservations/{id}` | CONSUMER | 상세 조회 (visitCode 포함) |
| 4 | GET | `/api/reservations/merchant` | MERCHANT | 상태별 필터링 추가 |
| 5 | PATCH | `/api/flash-purchases/pickup` | MERCHANT | CONFIRMED → PICKED_UP |
| 6 | PATCH | `/api/flash-purchases/{id}/cancel` | MERCHANT | CONFIRMED → CANCELLED (재고 복원) |
| 7 | GET | `/api/flash-purchases/{id}` | CONSUMER | 상세 조회 (pickupCode 포함) |
| 8 | GET | `/api/flash-purchases/merchant` | MERCHANT | 소상공인 구매 목록 (상태 필터) |

## Estimated Effort

- Enum/Entity/Flyway: 30분
- 서비스 로직 (6개 메서드): 2시간
- 컨트롤러 (8개 엔드포인트): 1시간
- 스케줄러: 30분
- 테스트: 1.5시간
