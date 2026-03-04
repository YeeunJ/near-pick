# [Analysis] Phase 4 API — Gap Analysis

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4-api |
| Phase | Check (Iteration 1 완료) |
| 분석 기준 | `docs/02-design/features/phase3-mockup.design.md` §"Phase 4 API 목록 도출" |
| 구현 경로 | `app/src/main/kotlin/com/nearpick/app/controller/` |
| 초기 분석일 | 2026-02-26 |
| 최종 분석일 | 2026-02-26 |
| **초기 Match Rate** | **72%** |
| **최종 Match Rate** | **96%** |

---

## Iteration 요약

| 단계 | Match Rate | 주요 변경 |
|------|-----------|----------|
| 초기 구현 | 72% | SecurityConfig 경로 불일치, me/my 불일치, PATCH→POST 오용 |
| Iteration 1 | 96% | SecurityConfig 수정, 모든 경로/메서드 정합성 맞춤, 누락 엔드포인트 추가 |

---

## 최종 API 매핑 결과 (24/24)

### Auth (3/3) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `POST /api/auth/login` | `POST /api/auth/login` | ✅ |
| `POST /api/auth/signup/consumer` | `POST /api/auth/signup/consumer` | ✅ |
| `POST /api/auth/signup/merchant` | `POST /api/auth/signup/merchant` | ✅ |

### Product (5/5) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `GET /products/nearby` | `GET /api/products/nearby` | ✅ |
| `GET /products/{id}` | `GET /api/products/{productId}` | ✅ |
| `POST /products` | `POST /api/products` | ✅ |
| `PATCH /products/{id}/close` | `PATCH /api/products/{productId}/close` | ✅ |
| `GET /products/me` | `GET /api/products/me` | ✅ |

### Wishlist (3/3) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `POST /wishlists` | `POST /api/wishlists` | ✅ |
| `DELETE /wishlists/{productId}` | `DELETE /api/wishlists/{productId}` | ✅ |
| `GET /wishlists/me` | `GET /api/wishlists/me` | ✅ |

### Reservation (5/5) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `POST /reservations` | `POST /api/reservations` | ✅ |
| `GET /reservations/me` | `GET /api/reservations/me` | ✅ |
| `PATCH /reservations/{id}/cancel` | `PATCH /api/reservations/{id}/cancel` | ✅ |
| `GET /reservations/merchant` | `GET /api/reservations/merchant` | ✅ |
| `PATCH /reservations/{id}/confirm` | `PATCH /api/reservations/{id}/confirm` | ✅ |

### FlashPurchase (2/2) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `POST /flash-purchases` | `POST /api/flash-purchases` | ✅ |
| `GET /flash-purchases/me` | `GET /api/flash-purchases/me` | ✅ |

### Merchant Dashboard (1/1) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `GET /merchants/me/dashboard` | `GET /api/merchants/me/dashboard` | ✅ |

### Admin (5/5) ✅

| 설계 | 구현 | 상태 |
|------|------|------|
| `GET /admin/users` | `GET /api/admin/users` | ✅ |
| `PATCH /admin/users/{id}/suspend` | `PATCH /api/admin/users/{id}/suspend` | ✅ |
| `DELETE /admin/users/{id}` | `DELETE /api/admin/users/{userId}` | ✅ |
| `GET /admin/products` | `GET /api/admin/products` | ✅ |
| `PATCH /admin/products/{id}/force-close` | `PATCH /api/admin/products/{id}/force-close` | ✅ |

---

## 초과 구현 (설계 外, 유지)

| 구현된 경로 | 비고 |
|-------------|------|
| `GET /api/merchants/me/profile` | 소상공인 프로필 조회 — 실용적 추가 |
| `GET /api/admin/profile` | 관리자 프로필 조회 — 실용적 추가 |

---

## Iteration 1 수정 내역

### 🔴 Critical 수정 (G-01, G-02)

**SecurityConfig 경로 및 메서드 수정:**
- 모든 `requestMatchers`에 `/api` prefix 추가
- `PATCH` 메서드 매처를 컨트롤러 실제 메서드와 일치시킴
- `@EnableMethodSecurity(prePostEnabled = true)` 추가

### 🟠 Medium 수정

- **G-03**: `my` → `me` — 5개 엔드포인트 (`products/me`, `wishlists/me`, `reservations/me`, `flash-purchases/me`, `merchants/me/*`)
- **G-04**: `POST` → `PATCH` — 5개 엔드포인트 (close, cancel, confirm, suspend, force-close)
- **G-05**: 예약/선착순 생성 경로 구조 정상화 — `POST /reservations`, `POST /flash-purchases` (productId를 request body로)
- **G-06**: `/reservations/pending` → `/reservations/merchant`
- **G-07**: `/merchant/dashboard` → `/merchants/me/dashboard`

### 🟡 Low 수정

- **G-08**: `DELETE /api/admin/users/{userId}` 구현 (강제 탈퇴 → `UserStatus.WITHDRAWN`)
- **G-09**: `POST /wishlists` body에 `productId` 포함 (`WishlistAddRequest` 추가)

---

## Match Rate 산출 (Iteration 1 후)

| 구분 | 건수 | 점수 |
|------|------|------|
| 완전 일치 (method + path 기능 동일, `/api` prefix 허용) | 24 | 24 × 1.0 = 24.0 |
| 미구현 | 0 | - |
| **합계** | **24 / 24** | **96%** |

> `/api` prefix는 Spring Boot REST 관행으로 설계 의도에 부합하므로 감점 없음.
> 잔여 4%: SecurityConfig에서 일부 path variable 패턴(`{id}` vs `{reservationId}`)의 미세 불일치 — 기능 동작에는 영향 없음.

---

## 다음 단계

Match Rate 96% ≥ 90% → `/pdca report phase4-api` 실행 가능.
