# [Plan] Phase 1 — Schema / Terminology

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase1-schema |
| Phase | Plan |
| 작성일 | 2026-02-23 |
| 프로젝트 | NearPick |
| 레벨 | Enterprise |

---

## 1. 목표 (Objective)

NearPick 도메인의 핵심 용어와 데이터 구조를 정의한다.
모든 팀원과 AI가 동일한 언어로 소통할 수 있도록 명확한 스키마 기반을 확립한다.

---

## 2. 배경 (Background)

NearPick은 **지역 기반 실시간 인기 상품 커머스 플랫폼**이다.

- **소비자(Consumer):** 현재 위치 근처의 인기 상품을 탐색하고, 찜하거나 예약/선착순 구매를 요청
- **소상공인(Merchant):** 예약 상품 또는 할인권을 등록하여 노출 및 판매 기회를 확보

주요 비즈니스 특성:
- 위치(Location) 기반 상품 노출
- 실시간 인기도(Popularity) 반영
- 예약(Reservation) vs 선착순(Flash Sale) 두 가지 구매 방식
- 소비자-소상공인 양면 시장(Two-sided Marketplace)

---

## 3. 범위 (Scope)

### In Scope
- 핵심 도메인 엔티티 정의
- 비즈니스 용어 사전 (Ubiquitous Language)
- 엔티티 간 관계(ERD 수준)
- 핵심 속성 및 데이터 타입
- 비즈니스 규칙 (Business Rules)

### Out of Scope (이번 Phase)
- 실제 DB 마이그레이션 스크립트
- API 스펙 (Phase 4에서 다룸)
- UI 컴포넌트 (Phase 3/5에서 다룸)
- **배송(Delivery):** NearPick은 근처 상품을 직접 수령/방문하는 구조 → 배송 불필요

### Future Scope (후순위 — 이후 Phase에서 별도 계획)
- **Review:** 소비자의 상품/소상공인 리뷰 및 평점
- **Payment:** 결제 수단 연동 (PG 연동, 환불 등)
- **Notification:** 예약 확정, 선착순 마감 등 알림 (Push/SMS)

---

## 4. 핵심 도메인 엔티티 (초안)

### 4.1 사용자 측

| 엔티티 | 설명 | 핵심 속성 |
|--------|------|-----------|
| **User** | 모든 사용자의 공통 기반 계정 | id, email, passwordHash, role, status, createdAt |
| **UserRole** | 사용자 역할 구분 | CONSUMER, MERCHANT, ADMIN |
| **ConsumerProfile** | 소비자 전용 프로필 | userId, nickname, currentLocation |
| **MerchantProfile** | 소상공인 전용 프로필 | userId, businessName, businessRegNo, shopLocation, rating |
| **AdminProfile** | 관리자 전용 프로필 | userId, adminLevel, permissions |

### 4.2 상품 측

| 엔티티 | 설명 | 핵심 속성 |
|--------|------|-----------|
| **Product** | 소상공인이 등록한 상품/할인권 | id, merchantId, title, description, price, productType, status |
| **ProductType** | 상품 유형 | RESERVATION(예약), FLASH_SALE(선착순) |
| **ProductStatus** | 상품 상태 | DRAFT, ACTIVE, PAUSED, CLOSED |

### 4.3 거래 측

| 엔티티 | 설명 | 핵심 속성 |
|--------|------|-----------|
| **Reservation** | 예약 구매 요청 | id, userId, productId, reservedAt, status |
| **FlashPurchase** | 선착순 구매 | id, userId, productId, purchasedAt, status |
| **Wishlist** | 찜 목록 | id, userId, productId, createdAt |

### 4.4 위치/인기 측

| 엔티티 | 설명 | 핵심 속성 |
|--------|------|-----------|
| **Location** | 지리적 위치 | latitude, longitude, radius, regionName |
| **PopularityScore** | 실시간 인기도 | productId, score, calculatedAt, factors |

---

## 5. 용어 사전 (Ubiquitous Language) — 초안

| 한국어 용어 | 영어 용어 | 설명 |
|------------|----------|------|
| 찜 | Wishlist / Bookmark | 소비자가 관심 상품을 저장하는 행위 |
| 예약 상품 | Reservation Product | 소비자가 날짜/시간을 지정해 사전 구매 |
| 선착순 상품 | Flash Sale Product | 수량 한정, 먼저 구매 요청한 순서로 확정 |
| 할인권 | Discount Voucher | 특정 금액/비율 할인이 적용되는 구매권 |
| 소상공인 | Merchant | 상품을 등록하는 판매자 (MerchantProfile) |
| 소비자 | Consumer | 상품을 탐색·구매하는 사용자 (ConsumerProfile) |
| 관리자 | Admin | 플랫폼 전체를 관리·모니터링하는 운영자 (AdminProfile) |
| 직접 수령 | On-site Pickup | 배송 없이 소비자가 직접 방문해 수령 |
| 인기도 | Popularity Score | 조회수, 찜 수, 구매 수 등을 종합한 점수 |
| 근처 | Nearby | 사용자 위치 반경 N km 이내 |
| 노출 | Exposure | 소비자 피드에 상품이 보이는 것 |

---

## 6. 비즈니스 규칙 (Business Rules) — 초안

1. **위치 기반 노출:** 소비자의 현재 위치로부터 일정 반경 내 상품만 노출
2. **인기도 정렬:** 동일 반경 내에서는 인기도 점수(PopularityScore) 내림차순 정렬
3. **예약 가능 조건:** ACTIVE 상태의 RESERVATION 타입 상품만 예약 가능
4. **선착순 구매 조건:** ACTIVE 상태의 FLASH_SALE 타입 + 재고(stock) > 0 인 경우만 구매 가능
5. **찜 중복 방지:** 동일 소비자가 동일 상품에 중복 찜 불가
6. **소상공인 상품 등록:** 사업자 인증(businessRegNo 검증) 완료 후에만 등록 가능
7. **관리자 권한:** ADMIN 역할 User만 사용자 관리·소상공인 모니터링·상품 강제 비활성화 가능
8. **배송 없음:** 모든 거래는 소비자 직접 방문 수령(On-site Pickup) 방식으로 처리

---

## 7. 작업 목록 (Tasks)

- [ ] 용어 사전 완성 및 검토 (`docs/01-plan/terminology.md`)
- [ ] 엔티티 관계도 작성 (ERD)
- [ ] 핵심 속성 상세 정의 (`docs/01-plan/schema.md`)
- [ ] 비즈니스 규칙 확정
- [ ] Phase 2 Convention 준비

---

## 8. 완료 기준 (Definition of Done)

- [ ] 모든 핵심 엔티티가 정의됨
- [ ] 용어 사전이 작성되어 팀 내 합의됨
- [ ] 엔티티 간 관계가 명확히 표현됨
- [ ] 비즈니스 규칙이 문서화됨
- [ ] `docs/01-plan/schema.md`, `terminology.md` 생성 완료

---

## 9. 다음 단계

완료 후 → `/pdca design phase1-schema` 로 설계 문서 작성
