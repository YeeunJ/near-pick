# [Report] controller — Phase 8 완료 보고서

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | controller (Phase 8: Code Review & Quality) |
| 브랜치 | `feature/phase8-review` |
| 시작일 | 2026-03-04 |
| 완료일 | 2026-03-05 |
| **최종 Match Rate** | **98%** |
| 이터레이션 횟수 | 0 (첫 구현으로 90% 초과 달성) |
| 변경 파일 | 33개, +1,743 / -49 lines |

---

## 1. 개요

Phase 8은 전체 코드베이스 품질 검증 및 코드 리뷰 단계로, 이전 Phase에서 구현된 API 레이어의 기능 오작동, 성능 리스크, 일관성 위반을 수정하고 테스트를 보완했다. 설계 단계에서 도출된 9개 이슈와 UI 개발팀 피드백 5건(B-1~B-5)을 모두 구현 완료했으며, 추가로 갭 분석 과정에서 설계 문서에 미포함된 버그 3건을 추가 발견·수정했다.

---

## 2. 구현 요약

### 2-1. P1 — 기능 오작동 수정 (3건)

| Issue | 파일 | 내용 |
|-------|------|------|
| #1 | `AdminController.kt` | `withdrawUser()` 204 No Content → 200 + ApiResponse 반환으로 통일 |
| #2 | `WishlistServiceImpl.kt` | `remove()` 찜 없을 때 `PRODUCT_NOT_FOUND` → `RESOURCE_NOT_FOUND` 정정 |
| #3 | `GlobalExceptionHandler.kt` | `MissingServletRequestParameterException` (400) 핸들러 추가<br>+ `HttpMessageNotReadableException` (잘못된 enum/JSON 400) 핸들러 추가 |

> **#3 비고**: 설계에서 명시한 `HttpRequestMethodNotAllowedException` (405)는 Spring Boot 4.x(Spring 7.x)에서 해당 클래스가 제거됨. `DefaultHandlerExceptionResolver`가 405를 자동 처리하므로 커스텀 핸들러 불필요.

---

### 2-2. P2 — 성능 / 일관성 수정 (4건)

| Issue | 파일 | 내용 |
|-------|------|------|
| #4 | `ReservationEntity.kt` | `idx_reservations_product`, `idx_reservations_status` 인덱스 추가 |
| #4 | `FlashPurchaseEntity.kt` | `idx_flash_purchases_product` 인덱스 추가 |
| #5 | `WishlistController.kt`, `TransactionDtos.kt` | 임시 `mapOf()` → 타입 안전한 `WishlistAddResponse` DTO로 교체 |
| #6 | `WishlistRepository.kt`, `WishlistServiceImpl.kt` | 무제한 조회 → `findTop200ByUser_IdOrderByCreatedAtDesc()` 최대 200개 제한 |
| #7 | `ProductController.kt`, `FlashPurchaseController.kt` | `@Valid @RequestBody` → `@RequestBody @Valid` 전체 통일 |

---

### 2-3. P3 — 테스트 보완 (2건)

| Issue | 파일 | 테스트 케이스 |
|-------|------|------------|
| #8 | `WishlistServiceImplTest.kt` (신규) | 6개: add(성공/중복/상품없음), remove(성공/없음), getMyWishlists |
| #9 | `RateLimitFilterTest.kt` (신규) | 5개: 정상요청/429/loginBucket/signup/X-Forwarded-For |

---

### 2-4. UI 피드백 반영 (B-1~B-5)

| 항목 | 파일 | 변경 내용 |
|------|------|---------|
| B-1 | `AdminDtos.kt`, `ProductMapper.kt` | `AdminProductItem.price: Int` 추가 |
| B-2 | `TransactionDtos.kt`, `TransactionMapper.kt` | `WishlistItem.productStatus`, `shopAddress` 추가 |
| B-3 | `ProductDtos.kt`, `ProductNearbyProjection.kt`, `ProductRepository.kt`, `ProductMapper.kt` | `ProductSummaryResponse.shopAddress/shopLat/shopLng` 추가, native query SELECT 확장 |
| B-4 | `MerchantDtos.kt`, `MerchantServiceImpl.kt` | `MerchantDashboardResponse.recentReservations: List<ReservationItem>` 추가 |
| B-5 | `TransactionDtos.kt`, `TransactionMapper.kt` | `ReservationItem.memo: String?` 추가 |

---

### 2-5. 설계 외 추가 버그 픽스 (3건)

갭 분석 과정에서 별도 발견한 버그:

| 버그 | 원인 | 수정 |
|------|------|------|
| `ProductType.RESERVATION` 명명 오류 | 도메인 용어집/overview는 `GENERAL\|FLASH_SALE`이 올바른 설계 | `RESERVATION` → `GENERAL` rename + `V3__rename_product_type.sql` 마이그레이션 |
| `/api/products/nearby` 500 에러 | `ProductNearbyProjection.popularityScore: Double` vs MySQL `DECIMAL(10,4)` → `BigDecimal` type mismatch | `Double` → `BigDecimal` + mapper에서 `.toDouble()` 변환 |
| 잘못된 JSON 전송 시 500 | `HttpMessageNotReadableException` 핸들러 누락 | `GlobalExceptionHandler`에 400 핸들러 추가 |

---

## 3. 아키텍처 검증

| 검증 항목 | 결과 |
|---------|------|
| `app` → `domain-nearpick` 직접 import | ✅ 위반 없음 |
| 패키지 root 일관성 (`com.nearpick.*`) | ✅ 준수 |
| `@SpringBootApplication(scanBasePackages)` | ✅ `com.nearpick` 전체 스캔 |
| Pessimistic Lock (`findByIdWithLock`) | ✅ FlashPurchase 동시성 제어 |
| Batch Count Query (`countByProductIds`) | ✅ N+1 방지 |
| Rate Limit 경로 분기 | ✅ login/signup vs 일반 API 분리 |
| 보안 헤더 (HSTS, X-Frame-Options) | ✅ Phase 7에서 완료 |
| JWT Principal 타입 (`Long`) | ✅ 전체 컨트롤러 일관성 |

---

## 4. 테스트 현황

| 테스트 클래스 | 케이스 수 | 상태 |
|-------------|:--------:|:----:|
| `NearPickApplicationTests` | 1 | ✅ |
| `AuthServiceImplTest` | 5 | ✅ |
| `ReservationServiceImplTest` | 4 | ✅ |
| `FlashPurchaseServiceImplTest` | 3 | ✅ |
| `MerchantServiceImplTest` | 3 | ✅ |
| **`WishlistServiceImplTest`** (신규) | **6** | ✅ |
| `ProductControllerTest` | 4 | ✅ |
| `MerchantControllerTest` | 3 | ✅ |
| **`RateLimitFilterTest`** (신규) | **5** | ✅ |
| **합계** | **34** | ✅ |

---

## 5. 알려진 한계 (수정 보류)

| 항목 | 현황 | 계획 |
|------|------|------|
| `ProductServiceImpl.getDetail()` 3회 단건 쿼리 | wishlist/reservation/purchase 각 1회 | Phase 9 배포 시 모니터링 후 판단 |
| `RateLimitFilter` ConcurrentHashMap 무제한 성장 | IP별 Bucket 무한 축적 가능 | Phase 9 이후 Redis Bucket4j 전환 |
| Flyway V4 (인덱스 DDL) | Entity `@Index`는 추가됐으나 SQL 마이그레이션은 미작성 | Phase 9 배포 시 `V4__add_transaction_indexes.sql` 추가 |

---

## 6. 변경 파일 목록 (핵심)

```
app/
  config/
    GlobalExceptionHandler.kt          — HttpMessageNotReadable 핸들러 추가
  controller/
    AdminController.kt                 — withdrawUser 200 응답
    WishlistController.kt              — WishlistAddResponse 적용
    ProductController.kt               — @RequestBody @Valid 순서
    FlashPurchaseController.kt         — @RequestBody @Valid 순서
  test/
    config/RateLimitFilterTest.kt      — [신규] 5개 케이스
    controller/MerchantControllerTest  — 보완

domain/
  admin/dto/AdminDtos.kt               — AdminProductItem.price 추가
  merchant/dto/MerchantDtos.kt         — recentReservations 추가
  product/ProductType.kt               — RESERVATION → GENERAL
  product/dto/ProductDtos.kt           — ProductSummaryResponse 3필드 추가
  transaction/dto/TransactionDtos.kt   — WishlistAddResponse, WishlistItem 2필드, ReservationItem.memo

domain-nearpick/
  product/
    entity/                            — (기존)
    mapper/ProductMapper.kt            — popularityScore.toDouble(), 3필드 매핑
    repository/ProductNearbyProjection — popularityScore BigDecimal, 3필드 추가
    repository/ProductRepository.kt    — native query SELECT 확장
    service/ProductServiceImpl.kt      — (기존)
  transaction/
    entity/ReservationEntity.kt        — product_id, status 인덱스
    entity/FlashPurchaseEntity.kt      — product_id 인덱스
    mapper/TransactionMapper.kt        — WishlistItem, ReservationItem 매핑 추가
    repository/WishlistRepository.kt   — findTop200 메서드
    service/WishlistServiceImpl.kt     — RESOURCE_NOT_FOUND, findTop200 적용
    service/WishlistServiceImplTest.kt — [신규] 6개 케이스
  user/
    service/MerchantServiceImpl.kt     — recentReservations 로직 추가

app/resources/db/
  migration/V3__rename_product_type_reservation_to_general.sql  — [신규]
  testdata/V2__insert_dummy_data.sql   — RESERVATION → GENERAL
```

---

## 7. Gap Analysis 요약

| 구분 | 설계 항목 | 구현 | Match Rate |
|:----:|:--------:|:----:|:----------:|
| P1 이슈 | 3 | 3 | 100% |
| P2 이슈 | 4 | 4 | 100% |
| P3/테스트 | 2 | 2 | 100% |
| UI 피드백 | 5 | 5 | 100% |
| **전체** | **14** | **14** | **98%** |

> 2%는 Spring 7.x에서 `HttpRequestMethodNotAllowedException` 클래스가 제거돼 설계의 405 핸들러를 직접 추가할 수 없으나, Spring 내장 메커니즘이 동일 기능을 수행하므로 실질적 기능 달성.

---

## 8. 결론

Phase 8 Code Review & Quality 구현을 **Match Rate 98%로 완료**. 전체 9개 이슈 + 5건 UI 피드백이 모두 구현됐으며, 갭 분석 과정에서 추가 발견한 프로덕션 버그 3건(일반 상품 등록 불가, nearby 500 에러, 잘못된 enum 500 에러)도 함께 수정됐다.

---

## 9. 전체 프로젝트 로드맵 (Phase 8 완료 기준 재정의)

Phase 8은 Phase 1~8의 전체 코드베이스를 검토·완성하는 리뷰 단계였으며, 이를 계기로 이후 확장 로드맵을 재수립했다. 기존 Phase 9(배포)는 모든 기능 확장 완료 후로 이동한다.

### 서버 목표 성능

| 구분 | 목표 TPS |
|------|:--------:|
| 평시 | 200 |
| 이벤트 | 3,000 |
| 선착순 | 10,000 |

### 전체 Phase 로드맵

| Phase | 이름 | 상태 |
|-------|------|:----:|
| 1 | Schema / Terminology | ✅ |
| 2 | Coding Convention | ✅ |
| 2.5 | Docs & Workflow | ✅ |
| 3 | Screen Flow Mockup | ✅ |
| 4 | API Design & Implementation | ✅ |
| 4.5 | API Quality (Swagger + Test + Flyway) | ✅ |
| 5 | Design System | ✅ |
| 6 | UI + API Integration | ➡️ near-pick-web |
| 7 | Security | ✅ |
| **8** | **Code Review & Quality** | **✅ (현재)** |
| 9 | 고성능 아키텍처 (Redis, Kafka, 10K TPS) | ⏳ |
| 10 | 위치 & 지도 서비스 | ⏳ |
| 11 | 상품 고도화 (사진, 카테고리) | ⏳ |
| 12 | 구매 라이프사이클 정리 | ⏳ |
| 13 | 리뷰 시스템 + AI 검증 | ⏳ |
| 14 | 사용자 고도화 | ⏳ |
| 15 | 종합 QA & 배포 | ⏳ |

### 신규 Phase 요약

**Phase 9 — 고성능 아키텍처**
Redis 캐싱, Kafka 이벤트 스트리밍, DB Read Replica, Redisson Distributed Lock, Redis Bucket4j Rate Limiting, Resilience4j Circuit Breaker. 10,000 TPS 선착순 처리 보장.

**Phase 10 — 위치 & 지도 서비스**
주소 검색 API(카카오), 소비자 저장 위치 최대 5개 등록, 지도 API 연동(상품 핀 클러스터링), `nearby` 위치 소스 선택.

**Phase 11 — 상품 고도화**
S3 + CloudFront CDN 이미지 업로드(최대 5장), 카테고리 체계(음식/음료/뷰티/생활용품/기타), 음식 메뉴 옵션 시스템, 비음식 유연한 스펙 속성.

**Phase 12 — 구매 라이프사이클 정리**
예약 상태 플로우 확정(`PENDING→CONFIRMED→VISITED→COMPLETED/CANCELLED/NO_SHOW`), 선착순 구매 플로우, QR 체크인, 자동 상태 변경 스케줄러, 취소/환불 정책.

**Phase 13 — 리뷰 시스템 + AI 검증**
구매/방문 완료 후 리뷰 작성(별점+텍스트+이미지), 소상공인 답글, Claude API 연동 AI 검증(비속어·허위 리뷰 감지→자동 블라인드), 관리자 검토 큐, 상품 평점 자동 집계.

**Phase 14 — 사용자 고도화**
소비자 등급 체계(일반/단골/VIP), 소상공인 인증 레벨, 단골 기능(가게 즐겨찾기→신상품 알림), FCM/APNs 알림 기반, 관리자 세그먼트 조회.

**Phase 15 — 종합 QA & 배포**
k6/Gatling 부하 테스트(3개 TPS 시나리오), Kubernetes(EKS)+HPA, GitHub Actions→ArgoCD CI/CD, Prometheus+Grafana+Loki+Jaeger 모니터링, RDS Aurora MySQL Multi-AZ, CloudFront.
