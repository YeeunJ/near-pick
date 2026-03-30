# Plan: phase13-review-system

## Feature Overview

구매·방문 완료 후 소비자가 리뷰를 작성하고, **Claude AI**가 비속어·허위 리뷰를 자동 감지해 블라인드 처리하는
**리뷰 시스템 전체**를 구현한다.
상품 평점 자동 집계, 소상공인 답글, 리뷰 신고 → 관리자 검토 큐까지 포함한다.

---

## Background & 현재 상태 분석

### Phase 12 이후 기준

| 영역 | 현재 상태 | Phase 13에서 해결 |
|------|----------|-----------------|
| Reservation 완료 | `status = COMPLETED` 도달 | 리뷰 작성 진입점 |
| FlashPurchase 완료 | `status = PICKED_UP` 도달 | 리뷰 작성 진입점 |
| 리뷰 엔티티 | 없음 | 신규 구현 |
| 상품 평점 | `ProductEntity`에 없음 | averageRating, reviewCount 추가 |
| AI 연동 | 없음 | Claude API (비속어·허위 리뷰 감지) |
| 관리자 리뷰 관리 | 없음 | 블라인드·신고 검토 큐 |

---

## Scope

---

### 기능 1: 리뷰 엔티티 설계

#### 1-1. ReviewEntity

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `user` | UserEntity | 작성자 (CONSUMER) |
| `product` | ProductEntity | 대상 상품 |
| `reservationId` | Long? | 연관 예약 ID (Reservation 리뷰 시) |
| `flashPurchaseId` | Long? | 연관 선착순 구매 ID (FlashPurchase 리뷰 시) |
| `rating` | Int | 별점 1~5 |
| `content` | String | 리뷰 본문 (최대 500자) |
| `status` | ReviewStatus | ACTIVE / BLINDED / DELETED |
| `aiChecked` | Boolean | AI 검증 완료 여부 |
| `aiResult` | String? | AI 판정 결과 ("pass" / "fail" / "need_review") |
| `blindedReason` | String? | 블라인드 사유 (AI_DETECTED / ADMIN_REVIEWED) |
| `blindPending` | Boolean | need_review 판정 → 관리자 큐 대기 여부 |
| `createdAt` | LocalDateTime | 작성일 |

**제약:**
- `reservationId`, `flashPurchaseId` 중 하나만 non-null (CHECK)
- `reservationId`에 UNIQUE 제약 → 예약 1건당 리뷰 1개
- `flashPurchaseId`에 UNIQUE 제약 → 구매 1건당 리뷰 1개

#### 1-2. ReviewImageEntity

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `review` | ReviewEntity | 부모 리뷰 |
| `imageUrl` | String | S3 URL |
| `displayOrder` | Int | 표시 순서 (0-based) |

**제약:** 리뷰당 최대 3장

#### 1-3. ReviewReplyEntity

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `review` | ReviewEntity | 부모 리뷰 |
| `merchant` | MerchantProfileEntity | 답글 작성 소상공인 |
| `content` | String | 답글 본문 (최대 300자) |
| `createdAt` | LocalDateTime | 작성일 |

**제약:** 리뷰당 답글 1개만 허용 (`review_id` UNIQUE)

#### 1-4. ReviewStatus enum

```kotlin
enum class ReviewStatus {
    ACTIVE,    // 정상 노출
    BLINDED,   // AI 또는 관리자 블라인드
    DELETED,   // 소비자 삭제 (soft delete)
}
```

---

### 기능 2: 리뷰 작성 자격 검증

**작성 가능 조건:**

| 거래 타입 | 조건 | 연관 ID |
|----------|------|---------|
| Reservation | `status = COMPLETED` | `reservationId` |
| FlashPurchase | `status = PICKED_UP` | `flashPurchaseId` |

**불가 조건:**
- 이미 해당 거래에 리뷰가 존재 → `REVIEW_ALREADY_EXISTS`
- 거래가 완료 상태가 아님 → `REVIEW_NOT_ELIGIBLE`
- 본인 거래가 아님 → `FORBIDDEN`

---

### 기능 3: AI 리뷰 검증 (Claude API)

**처리 흐름:**

```
리뷰 작성 요청
  ↓
DB 저장 (status=ACTIVE, aiChecked=false, blindPending=false)
  ↓
즉시 응답 반환
  ↓ (비동기: @Async)
Claude API 호출
  ↓
AI 판정 3-way:
  - "pass"        → aiChecked=true, aiResult="pass"
  - "fail"        → aiChecked=true, aiResult="fail",
                    status=BLINDED, blindedReason="AI_DETECTED"
  - "need_review" → aiChecked=true, aiResult="need_review",
                    blindPending=true (관리자 큐 노출)
  - 타임아웃/오류  → aiChecked=false 유지, blindPending=true (안전 fallback)
```

**프롬프트 설계:**

```
[System]
너는 전자상거래 플랫폼 리뷰 정책 심사관이다.
제공된 상품명·카테고리·리뷰 텍스트·OCR 텍스트만 사용해 판단한다.
외부 지식이나 추론은 사용하지 않는다.

판단 기준:
- fail       : 비속어/욕설, 특정인 비방, 개인정보 노출, 광고성 스팸, 무의미 반복 텍스트
- pass       : 그 외 정상 리뷰
- need_review: 경계선 사례 또는 맥락이 불분명한 경우

반드시 아래 JSON만 출력한다. 다른 텍스트는 절대 포함하지 않는다:
{"result":"pass"|"fail"|"need_review","reason":"fail·need_review 시 한 문장 필수, pass 시 null"}

[User]
상품명: {productTitle}
카테고리: {category}
리뷰: {reviewContent}
OCR: {ocrText}
```

**이미지 처리 방침:**
- 이미지 자체는 Claude에 직접 전달하지 않음
- 리뷰 이미지 업로드 시 OCR 텍스트 추출 (Tesseract or AWS Textract) → 텍스트로 전달
- OCR 결과 없으면 `ocrText: "(없음)"` 전달

**모델:** `claude-haiku-4-5-20251001` (속도/비용 최적, 텍스트 전용)

**Spring 연동:**
- `@Async` 메서드로 AI 검증 실행 (별도 스레드풀 `reviewAiExecutor`)
- 타임아웃: 5초 → 초과 시 `blindPending=true` fallback
- `@EnableAsync` 추가 (`NearPickApplication.kt`)
- `anthropic.api-key` → `application.properties` 외부화 (`@Value`)

**관리자 큐 노출 조건:** `blindPending=true` OR `reportCount >= 3`

---

### 기능 4: 상품 평점 자동 집계

**ProductEntity 변경:**

```kotlin
var averageRating: BigDecimal = BigDecimal.ZERO  // DECIMAL(3,2)
var reviewCount: Int = 0
```

**집계 갱신 시점:**
- 리뷰 작성 → `updateProductRating(productId)` 즉시 호출
- 리뷰 삭제(DELETED) → `updateProductRating(productId)` 재계산
- AI 블라인드(BLINDED) → BLINDED 리뷰는 평점 집계에서 제외

**집계 쿼리:**
```sql
SELECT AVG(rating), COUNT(*)
FROM reviews
WHERE product_id = ? AND status = 'ACTIVE'
```

---

### 기능 5: 소상공인 답글

**규칙:**
- 리뷰 1개당 답글 1개 (중복 불가)
- 상품 소유 소상공인만 작성 가능
- 답글 수정 불가 (삭제 후 재작성)
- BLINDED 리뷰에도 답글 작성 가능 (소상공인 입장 표명)

---

### 기능 6: 리뷰 신고 → 관리자 검토 큐

**처리 흐름:**
```
소비자 신고 → reviews.reportCount + 1
reportCount >= 3 → 관리자 알림 큐 (pending_review 상태로 관리자 목록에 노출)
관리자 블라인드 처리 → status=BLINDED, blindedReason="ADMIN_REVIEWED"
```

**ReviewEntity 추가 필드:**
- `reportCount: Int = 0`

**신고는 별도 테이블 없이 카운트만 관리** (이유: 신고자 목록 관리는 Phase 14 이후 범위)

---

### 기능 7: 신규 API 목록

| # | 메서드 | 엔드포인트 | 역할 | 설명 |
|---|--------|-----------|------|------|
| 1 | POST | `/api/reviews` | CONSUMER | 리뷰 작성 (이미지 포함) |
| 2 | GET | `/api/reviews/product/{productId}` | PUBLIC | 상품 리뷰 목록 (페이징, 정렬) |
| 3 | GET | `/api/reviews/me` | CONSUMER | 내 리뷰 목록 |
| 4 | DELETE | `/api/reviews/{reviewId}` | CONSUMER | 리뷰 삭제 (soft delete) |
| 5 | POST | `/api/reviews/{reviewId}/images` | CONSUMER | 리뷰 이미지 Presigned URL 발급 |
| 6 | POST | `/api/reviews/{reviewId}/reply` | MERCHANT | 소상공인 답글 작성 |
| 7 | DELETE | `/api/reviews/{reviewId}/reply` | MERCHANT | 소상공인 답글 삭제 |
| 8 | POST | `/api/reviews/{reviewId}/report` | CONSUMER | 리뷰 신고 |
| 9 | GET | `/api/admin/reviews` | ADMIN | 관리자 리뷰 목록 (blindPending=true OR reportCount>=3 필터) |
| 10 | PATCH | `/api/admin/reviews/{reviewId}/blind` | ADMIN | 관리자 수동 블라인드 |
| 11 | PATCH | `/api/admin/reviews/{reviewId}/unblind` | ADMIN | 관리자 블라인드 해제 |

---

### 기능 8: 신규 ErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `REVIEW_NOT_FOUND` | 404 | 리뷰를 찾을 수 없습니다. |
| `REVIEW_ALREADY_EXISTS` | 422 | 이미 해당 거래에 대한 리뷰가 존재합니다. |
| `REVIEW_NOT_ELIGIBLE` | 422 | 구매 또는 방문 완료 후에만 리뷰를 작성할 수 있습니다. |
| `REVIEW_BLINDED` | 403 | 블라인드 처리된 리뷰입니다. |
| `REVIEW_REPLY_ALREADY_EXISTS` | 422 | 이미 답글이 존재합니다. |
| `REVIEW_REPLY_NOT_FOUND` | 404 | 답글을 찾을 수 없습니다. |
| `REVIEW_IMAGE_LIMIT_EXCEEDED` | 422 | 리뷰 이미지는 최대 3장까지 등록 가능합니다. |

---

### 기능 9: Flyway V7 마이그레이션

```sql
-- reviews
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    reservation_id BIGINT NULL UNIQUE,
    flash_purchase_id BIGINT NULL UNIQUE,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ai_checked BOOLEAN NOT NULL DEFAULT FALSE,
    ai_result VARCHAR(20) NULL,           -- 'pass' | 'fail' | 'need_review'
    blinded_reason VARCHAR(100) NULL,     -- 'AI_DETECTED' | 'ADMIN_REVIEWED'
    blind_pending BOOLEAN NOT NULL DEFAULT FALSE,  -- need_review / fallback → 관리자 큐
    report_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- review_images
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (review_id) REFERENCES reviews(id)
);

-- review_replies
CREATE TABLE review_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    content VARCHAR(300) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(user_id)
);

-- products 평점 집계 필드 추가
ALTER TABLE products
    ADD COLUMN average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

-- 인덱스
CREATE INDEX idx_reviews_product_id ON reviews (product_id, status);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_reviews_report_count ON reviews (report_count);
```

---

## Out of Scope

- 리뷰 수정 (삭제 후 재작성만 허용)
- 리뷰 좋아요 / 도움이 됐어요 기능
- 신고자 이력 테이블 (Phase 14 이후)
- AI 재학습 / 피드백 루프
- 관리자 리뷰 일괄 처리 (개별 처리만)
- 리뷰 검색 (Phase 14 이후)

---

## Implementation Order

1. **Enum 추가** — `ReviewStatus` (ACTIVE / BLINDED / DELETED)
2. **ErrorCode 추가** — 7개
3. **Entity 설계** — ReviewEntity, ReviewImageEntity, ReviewReplyEntity
4. **Flyway V7** — 스키마 마이그레이션
5. **ProductEntity 변경** — averageRating, reviewCount 필드 추가
6. **Repository** — ReviewRepository, ReviewImageRepository, ReviewReplyRepository
7. **Domain 서비스 인터페이스** — ReviewService, ReviewImageService
8. **DTO 설계** — Request/Response DTO 전체
9. **Claude API 연동** — ReviewAiService (클린 아키텍처: domain 인터페이스 + domain-nearpick 구현)
10. **ServiceImpl 구현** — ReviewServiceImpl (작성·삭제·신고·집계), ReviewReplyServiceImpl
11. **@EnableAsync 추가** — NearPickApplication.kt
12. **컨트롤러** — ReviewController, AdminReviewController
13. **테스트 작성** — 리뷰 작성 자격·평점 집계·AI 검증 단위 테스트

---

## Success Criteria

- Reservation COMPLETED / FlashPurchase PICKED_UP 상태만 리뷰 작성 가능
- 거래 1건당 리뷰 중복 불가 (UNIQUE 제약)
- 리뷰 작성 후 Claude API 비동기 검증 실행 → 비속어 감지 시 BLINDED 전환
- 상품 평점(averageRating) ACTIVE 리뷰 기준 자동 집계
- 소상공인 답글 1건 제한 정상 작용
- 신고 3회 이상 → 관리자 목록 노출
- 관리자 수동 블라인드/해제 API 동작
- 기존 190개 테스트 GREEN 유지, 신규 테스트 추가

---

## New API Summary

| # | 메서드 | 엔드포인트 | 역할 |
|---|--------|-----------|------|
| 1 | POST | `/api/reviews` | CONSUMER |
| 2 | GET | `/api/reviews/product/{productId}` | PUBLIC |
| 3 | GET | `/api/reviews/me` | CONSUMER |
| 4 | DELETE | `/api/reviews/{reviewId}` | CONSUMER |
| 5 | POST | `/api/reviews/{reviewId}/images` | CONSUMER |
| 6 | POST | `/api/reviews/{reviewId}/reply` | MERCHANT |
| 7 | DELETE | `/api/reviews/{reviewId}/reply` | MERCHANT |
| 8 | POST | `/api/reviews/{reviewId}/report` | CONSUMER |
| 9 | GET | `/api/admin/reviews` | ADMIN |
| 10 | PATCH | `/api/admin/reviews/{reviewId}/blind` | ADMIN |
| 11 | PATCH | `/api/admin/reviews/{reviewId}/unblind` | ADMIN |

---

## Estimated Effort

| 항목 | 예상 시간 |
|------|---------|
| Enum / ErrorCode / Entity / Flyway | 40분 |
| ProductEntity 변경 + 평점 집계 | 30분 |
| Repository (3개) | 30분 |
| Claude API 연동 (ReviewAiService) | 1시간 |
| ReviewService + ReviewReplyService 구현 | 2시간 |
| 컨트롤러 (11개 엔드포인트) | 1시간 |
| 테스트 | 1.5시간 |
| **합계** | **~7시간** |
