# [Report] Phase 1 — Schema / Terminology 완료 보고서

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase1-schema |
| 완료일 | 2026-02-23 |
| 최종 Match Rate | **97%** |
| PDCA 반복 횟수 | 1회 |
| 상태 | **COMPLETED** |

---

## PDCA 전체 흐름

```
[Plan] ✅ → [Design] ✅ → [Do] ✅ → [Check] ✅ → [Act-1] ✅ → [Report] ✅
  계획 수립      설계 문서     엔티티 구현   Gap 분석     Gap 수정      완료
```

---

## 1. Plan 요약

**목표:** NearPick 도메인의 핵심 용어와 데이터 구조 정의

**서비스 정의:**
- NearPick = 지역 기반 실시간 인기 상품 커머스 플랫폼
- 소비자: 근처 인기 상품 탐색 → 찜 / 예약 / 선착순 구매
- 소상공인: 상품·할인권 등록 → 노출 및 판매 기회 확보
- 관리자: 플랫폼 사용자·소상공인 관리 및 모니터링

**핵심 결정:**
- 배송 없음 (직접 방문 수령 모델)
- 리뷰·결제·알림은 Future Scope으로 분류

---

## 2. Design 요약

### 도메인 구조 (3개 도메인)

```
사용자 도메인
  User ──1:1──> ConsumerProfile
       ──1:1──> MerchantProfile
       ──1:1──> AdminProfile

상품 도메인
  MerchantProfile ──1:N──> Product ──1:1──> PopularityScore

거래 도메인
  ConsumerProfile ──1:N──> Wishlist       ──N:1──> Product
                  ──1:N──> Reservation    ──N:1──> Product
                  ──1:N──> FlashPurchase  ──N:1──> Product
```

### 핵심 설계 결정 (ADR)

| ADR | 결정 | 이유 |
|-----|------|------|
| ADR-001 | Table Per Type | 역할별 속성 명확히 분리, NULL 컬럼 방지 |
| ADR-002 | DECIMAL(10,7) 위치 저장 | PostGIS 없이 반경 쿼리 가능, 추후 확장 용이 |
| ADR-003 | 인기도 별도 테이블 + 배치 | 실시간 계산 성능 저하 방지 |
| ADR-004 | 배송 없음 | 근처 방문 수령 모델, visit_scheduled_at으로 처리 |

---

## 3. Do 요약 — 생성 파일 목록

### 의존성 추가 (build.gradle.kts)

| 추가 항목 | 용도 |
|-----------|------|
| `kotlin("plugin.jpa")` | JPA 엔티티 no-arg 생성자 |
| `spring-boot-starter-web` | Web 레이어 |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `com.h2database:h2` | 개발용 인메모리 DB |
| `org.postgresql:postgresql` | 운영용 DB 드라이버 |

### 생성 파일 (16개)

**user 도메인 (7개)**
```
entity/  User.kt · ConsumerProfile.kt · MerchantProfile.kt · AdminProfile.kt
enums/   UserRole.kt · UserStatus.kt · AdminLevel.kt (Act-1 추가)
```

**product 도메인 (4개)**
```
entity/  Product.kt · PopularityScore.kt
enums/   ProductType.kt · ProductStatus.kt
```

**transaction 도메인 (5개)**
```
entity/  Wishlist.kt · Reservation.kt · FlashPurchase.kt
enums/   ReservationStatus.kt · FlashPurchaseStatus.kt
```

### 인덱스 선언 (7개)
```
idx_products_location       — shop_lat, shop_lng
idx_products_status_type    — status, product_type
idx_products_merchant       — merchant_id
idx_popularity_score        — score DESC
idx_wishlists_user          — user_id
idx_reservations_user       — user_id
idx_flash_purchases_user    — user_id
```

---

## 4. Check / Act 요약

### Gap 분석 결과 (Act-1 후)

| Gap | 심각도 | 상태 | 조치 |
|-----|--------|------|------|
| `AdminProfile.adminLevel` String → AdminLevel enum | MEDIUM | **해결** | `AdminLevel` enum 생성 + `@Enumerated` 적용 |
| `AdminProfile.permissions` TEXT → JSONB | MEDIUM | **부분 해결** | PostgreSQL 전환 시 `jsonb` 적용 안내 주석 추가 |
| 설계 문서 AdminLevel 누락 | LOW | **해결** | 패키지 구조에 `AdminLevel.kt` 추가 |

### Match Rate 변화
```
초기 Check: 92% → Act-1 후: 97%
```

---

## 5. 최종 검증

```
./gradlew test → BUILD SUCCESSFUL ✅
Hibernate DDL:  9개 테이블 정상 생성/삭제 확인 ✅
```

---

## 6. Future Scope (다음 단계 참고)

| 항목 | 내용 | 우선순위 |
|------|------|---------|
| Review | 구매 완료 후 평점/리뷰 엔티티 | 후순위 |
| Payment | PG 연동 결제 내역 엔티티 | 후순위 |
| Notification | Push/SMS 알림 엔티티 | 후순위 |
| JSONB 전환 | PostgreSQL 운영 전환 시 `AdminProfile.permissions` | DB 전환 시 |
| PostGIS 검토 | 복잡한 공간 쿼리 필요 시 | Phase 4 재검토 |

---

## 7. 다음 Pipeline 단계

Phase 1 완료. 다음은 **Phase 2: Coding Convention** 입니다.

```
/pdca plan phase2-convention
```

코딩 컨벤션, 네이밍 룰, 패키지 구조 규칙 등 NearPick 개발 표준을 정의합니다.
