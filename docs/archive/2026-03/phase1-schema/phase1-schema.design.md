# [Design] Phase 1 — Schema / Terminology

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase1-schema |
| Phase | Design |
| 작성일 | 2026-02-23 |
| 참조 | `docs/01-plan/features/phase1-schema.plan.md` |

---

## 1. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         사용자 도메인                                 │
│                                                                     │
│  ┌──────────────┐     1     ┌──────────────────┐                   │
│  │     User     │──────────>│  ConsumerProfile │                   │
│  │──────────────│           │──────────────────│                   │
│  │ id (PK)      │     1     │ user_id (FK, PK) │                   │
│  │ email        │──────────>│ nickname         │                   │
│  │ password_hash│           │ current_lat      │                   │
│  │ role         │     1     │ current_lng      │                   │
│  │ status       │──────────>│──────────────────│                   │
│  │ created_at   │           │  MerchantProfile │                   │
│  │ updated_at   │           │──────────────────│                   │
│  └──────────────┘           │ user_id (FK, PK) │                   │
│         │                   │ business_name    │                   │
│         │ 1                 │ business_reg_no  │                   │
│         ▼                   │ shop_lat         │                   │
│  ┌──────────────────┐       │ shop_lng         │                   │
│  │   AdminProfile   │       │ rating           │                   │
│  │──────────────────│       └──────────────────┘                   │
│  │ user_id (FK, PK) │                                              │
│  │ admin_level      │                                              │
│  │ permissions      │                                              │
│  └──────────────────┘                                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         상품 도메인                                   │
│                                                                     │
│  MerchantProfile ──1────────> Product <──────────── PopularityScore│
│                               │────────────────────│               │
│                               │ id (PK)            │               │
│                               │ merchant_id (FK)   │               │
│                               │ title              │               │
│                               │ description        │               │
│                               │ price              │               │
│                               │ product_type       │ ◄─── ENUM     │
│                               │ status             │ ◄─── ENUM     │
│                               │ stock              │               │
│                               │ available_from     │               │
│                               │ available_until    │               │
│                               │ shop_lat           │               │
│                               │ shop_lng           │               │
│                               │ created_at         │               │
│                               └────────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         거래 도메인                                   │
│                                                                     │
│  ConsumerProfile ──1──────N──> Wishlist <──────N──1── Product      │
│  ConsumerProfile ──1──────N──> Reservation <──N──1── Product       │
│  ConsumerProfile ──1──────N──> FlashPurchase <─N──1── Product      │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐ │
│  │    Wishlist      │  │   Reservation    │  │  FlashPurchase   │ │
│  │──────────────────│  │──────────────────│  │──────────────────│ │
│  │ id               │  │ id               │  │ id               │ │
│  │ user_id (FK)     │  │ user_id (FK)     │  │ user_id (FK)     │ │
│  │ product_id (FK)  │  │ product_id (FK)  │  │ product_id (FK)  │ │
│  │ created_at       │  │ reserved_at      │  │ purchased_at     │ │
│  │                  │  │ status           │  │ status           │ │
│  └──────────────────┘  │ visit_scheduled_ │  └──────────────────┘ │
│                        │   at             │                        │
│                        └──────────────────┘                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 엔티티 상세 설계

### 2.1 User

```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,  -- CONSUMER | MERCHANT | ADMIN
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUSPENDED | WITHDRAWN
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);
```

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | 자동 증가 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 식별자 |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt 해시 |
| role | VARCHAR(20) | NOT NULL | CONSUMER / MERCHANT / ADMIN |
| status | VARCHAR(20) | NOT NULL | ACTIVE / SUSPENDED / WITHDRAWN |

**설계 결정:**
- 소비자·소상공인·관리자 모두 `users` 테이블 단일 계정으로 관리
- 역할별 상세 정보는 별도 프로필 테이블로 분리 (Single Table Inheritance 대신 Table Per Type 채택)
- 향후 다중 역할 지원 시 `user_roles` 매핑 테이블 추가 확장 가능

---

### 2.2 ConsumerProfile

```sql
CREATE TABLE consumer_profiles (
    user_id      BIGINT       PRIMARY KEY REFERENCES users(id),
    nickname     VARCHAR(50)  NOT NULL,
    current_lat  DECIMAL(10,7),
    current_lng  DECIMAL(10,7),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);
```

| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT (FK) | users.id 참조, PK 겸용 |
| nickname | VARCHAR(50) | 표시 이름 |
| current_lat / lng | DECIMAL(10,7) | 마지막 확인 위치 (선택) |

---

### 2.3 MerchantProfile

```sql
CREATE TABLE merchant_profiles (
    user_id          BIGINT       PRIMARY KEY REFERENCES users(id),
    business_name    VARCHAR(100) NOT NULL,
    business_reg_no  VARCHAR(20)  NOT NULL UNIQUE,  -- 사업자등록번호
    shop_lat         DECIMAL(10,7) NOT NULL,
    shop_lng         DECIMAL(10,7) NOT NULL,
    shop_address     VARCHAR(255),
    rating           DECIMAL(3,2) DEFAULT 0.00,
    is_verified      BOOLEAN      NOT NULL DEFAULT false,
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);
```

| 컬럼 | 타입 | 설명 |
|------|------|------|
| business_reg_no | VARCHAR(20) | 사업자등록번호, 인증 완료 후 상품 등록 가능 |
| shop_lat / lng | DECIMAL(10,7) | 가게 위치 (상품 노출 기준) |
| is_verified | BOOLEAN | 사업자 인증 여부 |
| rating | DECIMAL(3,2) | 0.00 ~ 5.00 평균 평점 (Future) |

---

### 2.4 AdminProfile

```sql
CREATE TABLE admin_profiles (
    user_id     BIGINT      PRIMARY KEY REFERENCES users(id),
    admin_level VARCHAR(20) NOT NULL DEFAULT 'OPERATOR',  -- SUPER | OPERATOR
    permissions JSONB       NOT NULL DEFAULT '[]'
);
```

| 컬럼 | 타입 | 설명 |
|------|------|------|
| admin_level | VARCHAR(20) | SUPER(전체 권한) / OPERATOR(모니터링) |
| permissions | JSONB | 세분화 권한 목록 (예: ["USER_BAN", "PRODUCT_DEACTIVATE"]) |

---

### 2.5 Product

```sql
CREATE TABLE products (
    id               BIGSERIAL    PRIMARY KEY,
    merchant_id      BIGINT       NOT NULL REFERENCES merchant_profiles(user_id),
    title            VARCHAR(100) NOT NULL,
    description      TEXT,
    price            INT          NOT NULL,  -- 원 단위
    product_type     VARCHAR(20)  NOT NULL,  -- RESERVATION | FLASH_SALE
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    stock            INT          NOT NULL DEFAULT 0,
    available_from   TIMESTAMP,
    available_until  TIMESTAMP,
    shop_lat         DECIMAL(10,7) NOT NULL,  -- 수령 위치 (가게 위치 복사)
    shop_lng         DECIMAL(10,7) NOT NULL,
    view_count       INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);
```

**ProductType Enum**

| 값 | 설명 |
|----|------|
| `RESERVATION` | 날짜/시간 지정 예약 상품 |
| `FLASH_SALE` | 선착순 수량 한정 상품 |

**ProductStatus Enum**

| 값 | 설명 |
|----|------|
| `DRAFT` | 등록 중 (미공개) |
| `ACTIVE` | 공개·판매 중 |
| `PAUSED` | 일시 중단 |
| `CLOSED` | 판매 종료 |
| `FORCE_CLOSED` | 관리자 강제 비활성화 |

---

### 2.6 PopularityScore

```sql
CREATE TABLE popularity_scores (
    product_id      BIGINT    PRIMARY KEY REFERENCES products(id),
    score           DECIMAL(10,4) NOT NULL DEFAULT 0,
    view_weight     INT       NOT NULL DEFAULT 0,
    wishlist_weight INT       NOT NULL DEFAULT 0,
    purchase_weight INT       NOT NULL DEFAULT 0,
    calculated_at   TIMESTAMP NOT NULL DEFAULT now()
);
```

**점수 계산 공식 (초안):**
```
score = (view_count × 1) + (wishlist_count × 3) + (purchase_count × 5)
```

---

### 2.7 Wishlist

```sql
CREATE TABLE wishlists (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id),
    product_id  BIGINT    NOT NULL REFERENCES products(id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, product_id)  -- 중복 찜 방지
);
```

---

### 2.8 Reservation

```sql
CREATE TABLE reservations (
    id                  BIGSERIAL   PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users(id),
    product_id          BIGINT      NOT NULL REFERENCES products(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    visit_scheduled_at  TIMESTAMP,  -- 방문 예정 일시
    reserved_at         TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now()
);
```

**ReservationStatus Enum**

| 값 | 설명 |
|----|------|
| `PENDING` | 예약 요청 대기 |
| `CONFIRMED` | 소상공인 확정 |
| `CANCELLED` | 취소 |
| `VISITED` | 방문 완료 |

---

### 2.9 FlashPurchase

```sql
CREATE TABLE flash_purchases (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    product_id   BIGINT      NOT NULL REFERENCES products(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    purchased_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT now()
);
```

**FlashPurchaseStatus Enum**

| 값 | 설명 |
|----|------|
| `PENDING` | 구매 요청 (재고 선점) |
| `CONFIRMED` | 확정 |
| `CANCELLED` | 취소 |
| `COMPLETED` | 수령 완료 |

---

## 3. 인덱스 설계

```sql
-- 위치 기반 조회 (근처 상품 탐색 핵심)
CREATE INDEX idx_products_location ON products (shop_lat, shop_lng);
CREATE INDEX idx_products_status_type ON products (status, product_type);

-- 인기도 정렬
CREATE INDEX idx_popularity_score ON popularity_scores (score DESC);

-- 사용자별 찜/예약/구매 조회
CREATE INDEX idx_wishlists_user ON wishlists (user_id);
CREATE INDEX idx_reservations_user ON reservations (user_id);
CREATE INDEX idx_flash_purchases_user ON flash_purchases (user_id);

-- 소상공인별 상품 조회
CREATE INDEX idx_products_merchant ON products (merchant_id);
```

---

## 4. 도메인 패키지 구조 (Kotlin)

```
src/main/kotlin/com/nearpick/app/
├── domain/
│   ├── user/
│   │   ├── entity/
│   │   │   ├── User.kt
│   │   │   ├── ConsumerProfile.kt
│   │   │   ├── MerchantProfile.kt
│   │   │   └── AdminProfile.kt
│   │   └── enums/
│   │       ├── UserRole.kt
│   │       ├── UserStatus.kt
│   │       └── AdminLevel.kt
│   ├── product/
│   │   ├── entity/
│   │   │   ├── Product.kt
│   │   │   └── PopularityScore.kt
│   │   └── enums/
│   │       ├── ProductType.kt
│   │       └── ProductStatus.kt
│   └── transaction/
│       ├── entity/
│       │   ├── Wishlist.kt
│       │   ├── Reservation.kt
│       │   └── FlashPurchase.kt
│       └── enums/
│           ├── ReservationStatus.kt
│           └── FlashPurchaseStatus.kt
```

---

## 5. 핵심 설계 결정 (ADR)

### ADR-001: Table Per Type vs Single Table Inheritance
- **결정:** Table Per Type (별도 프로필 테이블)
- **이유:** 각 역할의 속성이 명확히 다름. STI 사용 시 NULL 컬럼 다수 발생
- **트레이드오프:** JOIN 필요하지만 스키마 명확성 확보

### ADR-002: 위치 저장 방식
- **결정:** `DECIMAL(10,7)` 컬럼 쌍 (lat/lng)
- **이유:** PostGIS 없이도 간단한 반경 쿼리 가능. 추후 PostGIS 확장 용이
- **트레이드오프:** 복잡한 공간 쿼리에는 PostGIS가 더 효율적 (Phase 4에서 재검토)

### ADR-003: 인기도 점수 별도 테이블
- **결정:** `popularity_scores` 별도 테이블 + 배치 업데이트
- **이유:** 실시간 계산은 조회 성능 저하. 주기적 업데이트로 분리
- **트레이드오프:** 약간의 데이터 지연(Eventually Consistent) 허용

### ADR-004: 배송 없음
- **결정:** 배송 관련 엔티티 미포함
- **이유:** NearPick은 근처 상품 직접 수령 모델. `visit_scheduled_at`으로 방문 일정 관리

---

## 6. Future Scope 엔티티 (후순위)

| 엔티티 | 연관 도메인 | 비고 |
|--------|-----------|------|
| `Review` | transaction | 구매 완료 후 평점/리뷰 |
| `Payment` | transaction | PG 연동 결제 내역 |
| `Notification` | user | Push/SMS 알림 |

---

## 7. 완료 기준 체크

- [x] 모든 핵심 엔티티 ERD 작성
- [x] 각 엔티티 컬럼·타입·제약 정의
- [x] 엔티티 간 관계 명시
- [x] 인덱스 전략 수립
- [x] 도메인 패키지 구조 설계
- [x] 핵심 설계 결정(ADR) 기록

---

## 8. 다음 단계

`/pdca do phase1-schema` → Kotlin 엔티티 클래스 구현 시작
