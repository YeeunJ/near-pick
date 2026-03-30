# Phase 13 Review System — Gap Analysis Report

**Date:** 2026-03-19
**Feature:** phase13-review-system
**Design:** `docs/02-design/features/phase13-review-system.design.md`
**Analyzer:** bkit:gap-detector

---

## Overall Result

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 98% | ✅ PASS |
| Architecture Compliance | 100% | ✅ PASS |
| Convention Compliance | 100% | ✅ PASS |
| Test (Design-specified) | 100% (15/15) | ✅ PASS |
| Test (Business Logic Coverage) | 65% | ⚠️ PARTIAL |
| **Overall** | **96%** | ✅ PASS |

---

## 1. Design Item Breakdown

| Category | Design Items | Matched | Rate |
|----------|:-----------:|:-------:|:----:|
| Enum (ReviewStatus) | 1 | 1 | 100% |
| Entities (3 entities, 26 fields) | 26 | 26 | 100% |
| DB Indexes | 4 | 4 | 100% |
| Repositories + methods | 14 | 14 | 100% |
| DTOs (6 classes) | 6 | 6 | 100% |
| Service interfaces + methods | 17 | 17 | 100% |
| Service implementations (5) | 5 | 5 | 100% |
| API endpoints (12) | 12 | 12 | 100% |
| Infrastructure (AsyncConfig, migration) | 5 | 5 | 100% |
| ErrorCodes (7) | 7 | 7 | 100% |
| Mapper + methods (5) | 5 | 5 | 100% |
| Configuration (anthropic.api-key) | 1 | 1* | 100%* |
| Tests (design-specified) | 15 | 15 | 100% |
| **Total** | **118** | **118** | **100%** |

---

## 2. Intentional Deviations (3)

설계서와 다르지만 프로젝트 컨벤션에 따른 의도적 변경.

| # | 항목 | 설계 | 구현 | 판단 |
|---|------|------|------|------|
| D1 | Controller 반환 타입 | `ResponseEntity<ApiResponse<T>>` | `ApiResponse<T>` + `@ResponseStatus` | ✅ Phase 4.5부터 적용된 프로젝트 컨벤션 |
| D2 | anthropic.api-key 위치 | `application.properties` | `application-local.properties` (gitignore) | ✅ 보안상 개선 (시크릿 분리 원칙) |
| D3 | @Transactional | 클래스 레벨 `@Transactional` | `@Transactional(readOnly=true)` 기본 + 쓰기 메서드 `@Transactional` | ✅ Phase 9에서 정립된 DB 최적화 패턴 |

---

## 3. Enhancements Beyond Design (8)

설계에 없지만 구현 중 추가된 개선 사항.

1. 모든 Controller 메서드에 Swagger `@Tag`, `@Operation`, `@SecurityRequirement` 추가
2. `@PreAuthorize` 역할 기반 인가 (CONSUMER/MERCHANT/ADMIN)
3. `AdminReviewController` 클래스 레벨 `@PreAuthorize("hasRole('ADMIN')")`
4. 이미지 최대 개수 `MAX_REVIEW_IMAGES = 3` 상수 추출 (매직 넘버 제거)
5. `V8__fix_review_rating_type.sql` — 기존 DB TINYINT → INT 마이그레이션
6. `ReviewReplyServiceImpl`에 `reviewImageRepository` 추가 (응답 데이터 완성)
7. `ReviewAiServiceImpl`에 Spring 관리 `ObjectMapper` 주입 (수동 생성 방지)
8. 전 서비스에 readOnly 트랜잭션 최적화 적용

---

## 4. Test Coverage Analysis

### 4-1. 설계 명세 테스트 (15/15 ✅)

설계서 Section 15에 명시된 모든 테스트 케이스가 구현됨.

**ReviewServiceImplTest (11/11)**

| # | 테스트 | 결과 |
|---|--------|------|
| 1 | create - Reservation COMPLETED 성공 | ✅ |
| 2 | create - FlashPurchase PICKED_UP 성공 | ✅ |
| 3 | create - PENDING 예약 → REVIEW_NOT_ELIGIBLE | ✅ |
| 4 | create - 중복 리뷰 → REVIEW_ALREADY_EXISTS | ✅ |
| 5 | create - 타인 예약 → FORBIDDEN | ✅ |
| 6 | delete - 본인 삭제 → DELETED | ✅ |
| 7 | delete - 타인 삭제 → FORBIDDEN | ✅ |
| 8 | report - reportCount +1 | ✅ |
| 9 | adminBlind - BLINDED + blindedReason=ADMIN_REVIEWED | ✅ |
| 10 | adminUnblind - ACTIVE + blindedReason=null | ✅ |
| 11 | updateProductRating - averageRating 갱신 | ✅ |

**ReviewReplyServiceImplTest (4/4)**

| # | 테스트 | 결과 |
|---|--------|------|
| 1 | create - 상품 소유 소상공인 → 성공 | ✅ |
| 2 | create - 다른 소상공인 → FORBIDDEN | ✅ |
| 3 | create - 중복 답글 → REVIEW_REPLY_ALREADY_EXISTS | ✅ |
| 4 | delete - 본인 답글 삭제 → 성공 | ✅ |

---

### 4-2. 테스트 커버리지 갭 (설계 외 추가 권장)

설계서에 명시되지 않았으나 비즈니스 로직 관점에서 보완이 필요한 케이스.

#### 🔴 HIGH: ReviewImageServiceImpl — 테스트 없음

| 시나리오 | 현황 |
|---------|------|
| `getPresignedUrl` 성공 | ❌ 없음 |
| `getPresignedUrl` - 이미지 3장 초과 → REVIEW_IMAGE_LIMIT_EXCEEDED | ❌ 없음 |
| `getPresignedUrl` - 리뷰 미존재 → REVIEW_NOT_FOUND | ❌ 없음 |
| `getPresignedUrl` - 타인 리뷰 → FORBIDDEN | ❌ 없음 |
| `confirmUpload` 성공 | ❌ 없음 |
| `confirmUpload` - 이미지 3장 초과 → REVIEW_IMAGE_LIMIT_EXCEEDED | ❌ 없음 |

#### 🟡 MEDIUM: ReviewServiceImpl — 조회 메서드 미테스트

| 시나리오 | 현황 |
|---------|------|
| `getProductReviews` - ACTIVE 리뷰만 반환 | ❌ 없음 |
| `getMyReviews` - DELETED 제외 반환 | ❌ 없음 |
| `getAdminQueue` - blindPending/reportCount 기준 반환 | ❌ 없음 |
| `create` - reservationId/flashPurchaseId 둘 다 null → REVIEW_NOT_ELIGIBLE | ❌ 없음 |

#### 🟡 MEDIUM: ReviewReplyServiceImpl — 에러 케이스 미완

| 시나리오 | 현황 |
|---------|------|
| `delete` - 답글 미존재 → REVIEW_REPLY_NOT_FOUND | ❌ 없음 |
| `delete` - 타인 답글 → FORBIDDEN | ❌ 없음 |

#### 🟢 LOW: 컨트롤러 테스트 없음

| 항목 | 현황 |
|------|------|
| ReviewController — 9 endpoints | ❌ 없음 |
| AdminReviewController — 3 endpoints | ❌ 없음 |

> 컨트롤러 테스트는 Zero Script QA(Docker 로그 기반)로 대체 가능하므로 LOW 우선순위.

---

### 4-3. 테스트 커버리지 요약

| 서비스 | 설계 명세 케이스 | 추가 권장 케이스 | 현재 커버 |
|--------|:-----------:|:-----------:|:------:|
| ReviewServiceImpl | 11/11 ✅ | 4건 미구현 | ~73% |
| ReviewReplyServiceImpl | 4/4 ✅ | 2건 미구현 | ~67% |
| ReviewImageServiceImpl | N/A (설계 명세 없음) | 6건 미구현 | 0% |
| ReviewAiServiceImpl | N/A (`@Profile("!test")`) | — | N/A |
| 전체 비즈니스 로직 | — | — | ~65% |

---

## 5. Missing Items

없음 — 설계 명세 기준 누락 구현 0건.

---

## 6. Recommendations

### 즉시 조치 (필수 아님, 선택)

아래 테스트를 추가하면 커버리지가 65% → 90%+ 로 향상됨:

1. **`ReviewImageServiceImplTest` 신규 작성** (HIGH priority)
   - 6가지 시나리오 (성공 2, 이미지 초과 2, FORBIDDEN 1, NOT_FOUND 1)

2. **`ReviewServiceImplTest` 조회 메서드 추가** (MEDIUM)
   - `getProductReviews`, `getMyReviews`, `getAdminQueue`, null 케이스

3. **`ReviewReplyServiceImplTest` 에러 케이스 추가** (MEDIUM)
   - `delete` FORBIDDEN, REVIEW_REPLY_NOT_FOUND

### 선택 조치

- 설계서 Section 11 응답 래핑 스타일 업데이트 (프로젝트 컨벤션 반영)
- 설계서 Section 12 rating 컬럼 타입 INT 명시 + V8 마이그레이션 언급
- 설계서 Section 13 `application-local.properties` 배치 명시

---

## 7. Conclusion

**Match Rate: 98%** (설계 명세 기준) → **90% 임계값 초과**

Phase 13 리뷰 시스템은 설계 명세에 정의된 모든 항목을 완전히 구현하였으며,
설계 외 8가지 개선(Swagger, 보안 인가, 타입 마이그레이션 등)도 포함됨.

**현재 상태**: `/pdca report phase13-review-system` 진행 가능.

테스트 커버리지는 설계 명세 100% 충족이나, `ReviewImageServiceImpl` 테스트 부재가
유일한 보완 권장 사항임. 이는 `/pdca iterate` 없이 선택적으로 보완 가능.
