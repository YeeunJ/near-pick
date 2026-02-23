# [Check] Phase 1 — Schema Gap Analysis

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase1-schema |
| Phase | Check |
| 분석일 | 2026-02-23 |
| Match Rate | **92% → 97%** (Act-1 후) |
| 결과 | PASS (>= 90%) |
| 반복 횟수 | 1회 |

---

## Match Rate

```
[Plan] ✅ → [Design] ✅ → [Do] ✅ → [Check] ✅ → [Act-1] ✅

Match Rate: 97% █████████████████████░ (PASS)
```

### Act-1 수정 내역 (2026-02-23)
- GAP-001 해결: `AdminLevel` enum 생성 + `AdminProfile.adminLevel` 타입 변경 (`String` → `AdminLevel`)
- GAP-002 부분 해결: `permissions` 컬럼에 PostgreSQL 전환 시 `jsonb` 적용 안내 주석 추가
- GAP-003 해결: 설계 문서 패키지 구조에 `AdminLevel.kt` 추가

---

## 체크 항목 결과 (35개)

### PASS (32/35)

| 항목 | 비고 |
|------|------|
| User 엔티티 구조 | 모든 필드·제약 일치 |
| User 컬럼 (id, email, password_hash, role, status, timestamps) | 완전 일치 |
| ConsumerProfile 엔티티 구조 | `@MapsId` OneToOne 정상 구현 |
| ConsumerProfile 컬럼 | user_id, nickname, current_lat/lng, updated_at 일치 |
| MerchantProfile 엔티티 구조 | `@MapsId` OneToOne 정상 구현 |
| MerchantProfile 컬럼 | 모든 컬럼 (business_reg_no UNIQUE 포함) 일치 |
| AdminProfile 엔티티 구조 | `@MapsId` OneToOne 정상 구현 |
| UserRole Enum (CONSUMER, MERCHANT, ADMIN) | 완전 일치 |
| UserStatus Enum (ACTIVE, SUSPENDED, WITHDRAWN) | 완전 일치 |
| Product 엔티티 구조 | MerchantProfile ManyToOne 정상 구현 |
| Product 컬럼 (전체) | 설계 DDL과 완전 일치 |
| Product 인덱스 3개 | idx_products_location, idx_products_status_type, idx_products_merchant 모두 선언 |
| ProductType Enum (RESERVATION, FLASH_SALE) | 완전 일치 |
| ProductStatus Enum (DRAFT, ACTIVE, PAUSED, CLOSED, FORCE_CLOSED) | 완전 일치 |
| PopularityScore 엔티티 구조 | `@MapsId` OneToOne 정상 구현 |
| PopularityScore 컬럼 | score, view/wishlist/purchase_weight, calculated_at 일치 |
| PopularityScore 인덱스 | idx_popularity_score (score DESC) 선언 |
| Wishlist 엔티티 구조 | User·Product ManyToOne 정상 구현 |
| Wishlist 컬럼 | 완전 일치 |
| Wishlist 유니크 제약 | `(user_id, product_id)` UNIQUE 선언 ✅ |
| Wishlist 인덱스 | idx_wishlists_user 선언 |
| Reservation 엔티티 구조 | User·Product ManyToOne 정상 구현 |
| Reservation 컬럼 | visit_scheduled_at 포함 완전 일치 |
| Reservation 인덱스 | idx_reservations_user 선언 |
| ReservationStatus Enum (PENDING, CONFIRMED, CANCELLED, VISITED) | 완전 일치 |
| FlashPurchase 엔티티 구조 | User·Product ManyToOne 정상 구현 |
| FlashPurchase 컬럼 | 완전 일치 |
| FlashPurchase 인덱스 | idx_flash_purchases_user 선언 |
| FlashPurchaseStatus Enum (PENDING, CONFIRMED, CANCELLED, COMPLETED) | 완전 일치 |
| ADR-001: Table Per Type | 별도 프로필 테이블 정상 구현 |
| ADR-002: DECIMAL(10,7) 위치 저장 | ConsumerProfile, MerchantProfile, Product 모두 적용 |
| ADR-003: 인기도 배치 계산 | 별도 테이블 + calculated_at 정상 구현 |
| ADR-004: 배송 없음 | visit_scheduled_at으로 방문 일정 처리 |
| 도메인 패키지 구조 | 설계 명세와 완전 일치 |

---

## Gap 목록 (3개)

### GAP-001 — MEDIUM
| 항목 | 내용 |
|------|------|
| **대상** | `AdminProfile.adminLevel` |
| **설계** | VARCHAR(20), ENUM 값: SUPER / OPERATOR |
| **구현** | `String` 타입 + 주석으로만 값 명시 |
| **문제** | 타입 안전성 없음. 잘못된 문자열 저장 가능 |
| **개선 방안** | `AdminLevel` enum 생성 + `@Enumerated(EnumType.STRING)` 적용 |

### GAP-002 — MEDIUM
| 항목 | 내용 |
|------|------|
| **대상** | `AdminProfile.permissions` |
| **설계** | JSONB 타입 (PostgreSQL 네이티브) |
| **구현** | `TEXT` columnDefinition |
| **문제** | PostgreSQL JSONB 인덱싱·연산자 활용 불가 |
| **개선 방안** | `columnDefinition = "jsonb"` 적용 (PostgreSQL 전환 시 활성화) |

### GAP-003 — LOW
| 항목 | 내용 |
|------|------|
| **대상** | 설계 문서 Section 4 패키지 구조 |
| **설계** | enum 목록에 AdminLevel 미포함 |
| **구현** | AdminLevel enum 미생성 |
| **문제** | GAP-001 수정 시 AdminLevel enum 추가 필요 |
| **개선 방안** | GAP-001 수정과 동시에 설계 문서 패키지 구조에 AdminLevel 추가 |

---

## 분석 요약

**전체 35개 체크 항목 중 32개 PASS, 2개 PARTIAL, 1개 FAIL**

9개 핵심 엔티티, 6개 Enum, 7개 인덱스, 4개 ADR 모두 정상 구현됨.
Gap은 `AdminProfile` 한 곳에 집중. 기능 동작에는 문제없으나 타입 안전성·DB 기능 활용 측면에서 개선 필요.

---

## 다음 단계

Match Rate 92% ≥ 90% 이므로 두 가지 선택:

1. **즉시 Report 진행:** `/pdca report phase1-schema`
2. **Gap 먼저 수정 후 Report:** GAP-001/002 수정 → `/pdca report phase1-schema`

GAP-001(AdminLevel enum)은 수정 비용이 낮고 코드 품질에 직접 영향을 주므로 수정을 권장합니다.
