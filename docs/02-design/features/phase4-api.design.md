# [Design] Phase 4 — API Design & Implementation

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4-api |
| Phase | Design |
| 작성일 | 2026-02-24 |
| 참조 | `docs/01-plan/features/phase4-api.plan.md`, `docs/02-design/features/phase3-mockup.design.md` |

---

## 1. 의존성 추가

### 1.1 app/build.gradle.kts

```kotlin
dependencies {
    // 기존
    implementation(project(":domain"))
    implementation(project(":common"))
    runtimeOnly(project(":domain-nearpick"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Phase 4 추가
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

### 1.2 domain/build.gradle.kts

```kotlin
dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Phase 4 추가 (DTO @Valid 지원)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

> `domain-nearpick/build.gradle.kts` 변경 없음 — JPA 의존성 이미 포함.

---

## 2. 파일 구조

### 2.1 전체 추가 파일 목록

```text
app/src/main/kotlin/com/nearpick/app/
├── NearPickApplication.kt
├── config/
│   ├── SecurityConfig.kt               ← FilterChain, BCrypt Bean, 경로 권한
│   ├── JwtTokenProvider.kt             ← 토큰 생성·검증
│   └── JwtAuthenticationFilter.kt      ← 요청당 토큰 파싱
└── controller/
    ├── AuthController.kt
    ├── ProductController.kt
    ├── WishlistController.kt
    ├── ReservationController.kt
    ├── FlashPurchaseController.kt
    ├── MerchantController.kt
    └── AdminController.kt

domain/src/main/kotlin/com/nearpick/domain/
├── auth/
│   ├── AuthService.kt                  ← interface
│   └── dto/
│       ├── SignupConsumerRequest.kt
│       ├── SignupMerchantRequest.kt
│       ├── LoginRequest.kt
│       └── TokenResponse.kt
├── product/
│   ├── ProductService.kt               ← interface
│   └── dto/
│       ├── ProductNearbyRequest.kt
│       ├── ProductSummaryResponse.kt
│       ├── ProductDetailResponse.kt
│       ├── ProductCreateRequest.kt
│       └── ProductListResponse.kt
├── wishlist/
│   ├── WishlistService.kt              ← interface
│   └── dto/
│       ├── WishlistAddRequest.kt
│       └── WishlistItemResponse.kt
├── reservation/
│   ├── ReservationService.kt           ← interface
│   └── dto/
│       ├── ReservationCreateRequest.kt
│       └── ReservationResponse.kt
├── flashpurchase/
│   ├── FlashPurchaseService.kt         ← interface
│   └── dto/
│       ├── FlashPurchaseRequest.kt
│       └── FlashPurchaseResponse.kt
├── merchant/
│   ├── MerchantService.kt              ← interface
│   └── dto/
│       └── DashboardResponse.kt
└── admin/
    ├── AdminService.kt                 ← interface
    └── dto/
        ├── UserSummaryResponse.kt
        └── AdminProductResponse.kt

domain-nearpick/src/main/kotlin/com/nearpick/nearpick/
├── auth/
│   └── AuthServiceImpl.kt
├── product/
│   ├── ProductServiceImpl.kt
│   └── ProductRepository.kt
├── wishlist/
│   ├── WishlistServiceImpl.kt
│   └── WishlistRepository.kt
├── reservation/
│   ├── ReservationServiceImpl.kt
│   └── ReservationRepository.kt
├── flashpurchase/
│   ├── FlashPurchaseServiceImpl.kt
│   └── FlashPurchaseRepository.kt
├── merchant/
│   └── MerchantServiceImpl.kt
├── admin/
│   └── AdminServiceImpl.kt
└── user/
    └── UserRepository.kt
```

---

## 3. JWT 설계

### 3.1 토큰 구조

| 필드 | 값 |
|------|----|
| Header | `alg: HS256`, `typ: JWT` |
| Payload | `sub` (userId), `role` (UserRole), `exp` (1시간) |
| Secret | `application.properties`의 `jwt.secret` (32자 이상) |

### 3.2 application.properties 추가 항목

```properties
# 운영 환경에서는 반드시 환경변수로 주입 (JWT_SECRET)
jwt.secret=${JWT_SECRET:change-me-in-production-must-be-32-chars-min}
jwt.expiration-ms=3600000
```

### 3.3 SecurityConfig — 경로 권한

| 경로 | 허용 역할 |
|------|-----------|
| `POST /auth/**` | 전체 허용 (permitAll) |
| `GET /products/nearby`, `GET /products/{id}` | 전체 허용 |
| `GET /products/me`, `POST /products`, `PATCH /products/*/close` | `MERCHANT` |
| `GET /merchants/me/dashboard` | `MERCHANT` |
| `GET /reservations/merchant`, `PATCH /reservations/*/confirm` | `MERCHANT` |
| `GET /reservations/me`, `POST /reservations`, `PATCH /reservations/*/cancel` | `CONSUMER` |
| `POST /wishlists`, `DELETE /wishlists/*`, `GET /wishlists/me` | `CONSUMER` |
| `POST /flash-purchases`, `GET /flash-purchases/me` | `CONSUMER` |
| `GET /admin/**`, `PATCH /admin/**`, `DELETE /admin/**` | `ADMIN` |

### 3.4 JwtAuthenticationFilter 흐름

```
요청 → Authorization: Bearer {token} 추출
  → JwtTokenProvider.validateToken()
  → claims에서 userId, role 파싱
  → UsernamePasswordAuthenticationToken 생성 → SecurityContext 저장
  → 다음 필터 진행
```

---

## 4. API 명세

### 4.1 공통 응답 형식

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": "에러 메시지" }
```

모든 Controller는 `ApiResponse<T>` (common 모듈)를 반환한다.

---

### 4.2 Auth (인증)

#### POST /auth/signup/consumer

**Request**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `201`:
```json
{ "success": true, "data": { "userId": 1, "email": "user@example.com", "role": "CONSUMER" } }
```

---

#### POST /auth/signup/merchant

**Request**:
```json
{
  "email": "shop@example.com",
  "password": "password123",
  "businessName": "카페 NearPick",
  "businessRegNo": "123-45-67890",
  "shopAddress": "서울 강남구 역삼로 123",
  "shopLat": 37.499591,
  "shopLng": 127.028307
}
```

**Response** `201`:
```json
{ "success": true, "data": { "userId": 2, "email": "shop@example.com", "role": "MERCHANT" } }
```

---

#### POST /auth/login

**Request**:
```json
{ "email": "user@example.com", "password": "password123" }
```

**Response** `200`:
```json
{ "success": true, "data": { "accessToken": "eyJ...", "tokenType": "Bearer" } }
```

---

### 4.3 Product (상품)

#### GET /products/nearby

**Query Params**: `lat`, `lng`, `radius` (km, default 5), `sort` (popularity|distance), `page` (default 0), `size` (default 20)

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "아메리카노",
        "price": 4000,
        "productType": "GENERAL",
        "status": "ACTIVE",
        "popularityScore": 127.5,
        "distanceKm": 0.8,
        "merchantName": "카페 NearPick"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0
  }
}
```

**Haversine Native Query** (ProductRepository):
```sql
SELECT p.*,
  (6371 * acos(cos(radians(:lat)) * cos(radians(p.shop_lat))
   * cos(radians(p.shop_lng) - radians(:lng))
   + sin(radians(:lat)) * sin(radians(p.shop_lat)))) AS distance_km
FROM product p
WHERE p.status = 'ACTIVE'
  AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.shop_lat))
       * cos(radians(p.shop_lng) - radians(:lng))
       + sin(radians(:lat)) * sin(radians(p.shop_lat)))) <= :radius
ORDER BY
  CASE WHEN :sort = 'distance' THEN distance_km END ASC,
  CASE WHEN :sort = 'popularity' THEN p.popularity_score END DESC
```

---

#### GET /products/{id}

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "아메리카노",
    "description": "진하고 맛있는 에스프레소",
    "price": 4000,
    "productType": "GENERAL",
    "status": "ACTIVE",
    "stock": 100,
    "availableFrom": "2026-03-01T09:00:00",
    "availableUntil": "2026-03-31T21:00:00",
    "shopLat": 37.499591,
    "shopLng": 127.028307,
    "shopAddress": "서울 강남구 역삼로 123",
    "merchantName": "카페 NearPick",
    "wishlistCount": 12,
    "reservationCount": 5,
    "purchaseCount": 3
  }
}
```

---

#### POST /products — 인증 필요 (MERCHANT)

**Request**:
```json
{
  "title": "딸기 케이크",
  "description": "신선한 딸기 케이크",
  "price": 6500,
  "productType": "FLASH_SALE",
  "stock": 10,
  "availableFrom": "2026-03-01T10:00:00",
  "availableUntil": "2026-03-01T18:00:00"
}
```

**Response** `201`:
```json
{ "success": true, "data": { "id": 5, "title": "딸기 케이크", "status": "DRAFT" } }
```

---

#### PATCH /products/{id}/close — 인증 필요 (MERCHANT)

**Response** `200`:
```json
{ "success": true, "data": { "id": 5, "status": "CLOSED" } }
```

---

#### GET /products/me — 인증 필요 (MERCHANT)

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1, "title": "아메리카노", "price": 4000,
        "status": "ACTIVE", "productType": "GENERAL",
        "stock": 100, "wishlistCount": 12
      }
    ],
    "totalElements": 3
  }
}
```

---

### 4.4 Wishlist (찜)

#### POST /wishlists — 인증 필요 (CONSUMER)

**Request**:
```json
{ "productId": 1 }
```

**Response** `201`:
```json
{ "success": true, "data": { "productId": 1, "addedAt": "2026-03-01T10:00:00" } }
```

---

#### DELETE /wishlists/{productId} — 인증 필요 (CONSUMER)

**Response** `204` (No Content)

---

#### GET /wishlists/me — 인증 필요 (CONSUMER)

**Response** `200`:
```json
{
  "success": true,
  "data": [
    {
      "productId": 1,
      "title": "아메리카노",
      "price": 4000,
      "merchantName": "카페 NearPick",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 4.5 Reservation (예약)

#### POST /reservations — 인증 필요 (CONSUMER)

**Request**:
```json
{
  "productId": 1,
  "visitAt": "2026-03-01T14:00:00",
  "quantity": 2,
  "memo": "창가 자리 부탁드립니다"
}
```

**Response** `201`:
```json
{
  "success": true,
  "data": { "id": 10, "status": "PENDING", "visitAt": "2026-03-01T14:00:00" }
}
```

---

#### GET /reservations/me — 인증 필요 (CONSUMER)

**Response** `200`:
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "productTitle": "아메리카노",
      "merchantName": "카페 NearPick",
      "status": "PENDING",
      "visitAt": "2026-03-01T14:00:00",
      "quantity": 2
    }
  ]
}
```

---

#### PATCH /reservations/{id}/cancel — 인증 필요 (CONSUMER)

**Response** `200`:
```json
{ "success": true, "data": { "id": 10, "status": "CANCELLED" } }
```

---

#### GET /reservations/merchant — 인증 필요 (MERCHANT)

**Query Params**: `status` (PENDING|CONFIRMED|CANCELLED, optional)

**Response** `200`: 위 GET /reservations/me와 동일 구조

---

#### PATCH /reservations/{id}/confirm — 인증 필요 (MERCHANT)

**Response** `200`:
```json
{ "success": true, "data": { "id": 10, "status": "CONFIRMED" } }
```

---

### 4.6 FlashPurchase (선착순 구매)

#### POST /flash-purchases — 인증 필요 (CONSUMER)

**Request**:
```json
{ "productId": 2, "quantity": 1 }
```

**Response** `201`:
```json
{
  "success": true,
  "data": { "id": 5, "status": "PENDING", "purchasedAt": "2026-03-01T12:00:00" }
}
```

**재고 처리**: `ProductEntity.stock`을 `stock - quantity`로 업데이트. `stock < quantity`이면 `ErrorCode.OUT_OF_STOCK` 반환.

---

#### GET /flash-purchases/me — 인증 필요 (CONSUMER)

**Response** `200`:
```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "productTitle": "딸기 케이크",
      "merchantName": "카페 NearPick",
      "status": "PENDING",
      "quantity": 1,
      "purchasedAt": "2026-03-01T12:00:00"
    }
  ]
}
```

---

### 4.7 Merchant Dashboard

#### GET /merchants/me/dashboard — 인증 필요 (MERCHANT)

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "businessName": "카페 NearPick",
    "todayReservationCount": 5,
    "todayPurchaseCount": 3,
    "popularityScore": 127.5,
    "pendingReservations": [
      {
        "id": 10,
        "consumerEmail": "user@example.com",
        "productTitle": "아메리카노",
        "visitAt": "2026-03-01T14:00:00"
      }
    ],
    "myProducts": [
      { "id": 1, "title": "아메리카노", "status": "ACTIVE", "productType": "GENERAL" }
    ]
  }
}
```

---

### 4.8 Admin

#### GET /admin/users — 인증 필요 (ADMIN)

**Query Params**: `role` (CONSUMER|MERCHANT|ADMIN, optional), `status` (ACTIVE|SUSPENDED, optional), `q` (email 검색, optional), `page`, `size`

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "content": [
      { "id": 1, "email": "user@example.com", "role": "CONSUMER", "status": "ACTIVE", "createdAt": "2026-01-01T00:00:00" }
    ],
    "totalElements": 100
  }
}
```

---

#### PATCH /admin/users/{id}/suspend — 인증 필요 (ADMIN)

**Response** `200`:
```json
{ "success": true, "data": { "id": 1, "status": "SUSPENDED" } }
```

---

#### DELETE /admin/users/{id} — 인증 필요 (ADMIN)

**Response** `204` (No Content)

---

#### GET /admin/products — 인증 필요 (ADMIN)

**Query Params**: `status` (ACTIVE|CLOSED|FORCE_CLOSED, optional), `page`, `size`

**Response** `200`:
```json
{
  "success": true,
  "data": {
    "content": [
      { "id": 1, "title": "아메리카노", "merchantName": "카페 NearPick", "status": "ACTIVE", "price": 4000 }
    ],
    "totalElements": 50
  }
}
```

---

#### PATCH /admin/products/{id}/force-close — 인증 필요 (ADMIN)

**Response** `200`:
```json
{ "success": true, "data": { "id": 1, "status": "FORCE_CLOSED" } }
```

---

## 5. ErrorCode 추가 목록

`common/ErrorCode.kt`에 아래를 추가한다:

| 코드 | HTTP | 메시지 |
|------|:----:|--------|
| `DUPLICATE_EMAIL` | 409 | Email already exists |
| `INVALID_CREDENTIALS` | 401 | Invalid email or password |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Access denied |
| `PRODUCT_NOT_FOUND` | 404 | Product not found |
| `USER_NOT_FOUND` | 404 | User not found |
| `RESERVATION_NOT_FOUND` | 404 | Reservation not found |
| `OUT_OF_STOCK` | 409 | Insufficient stock |
| `ALREADY_WISHLISTED` | 409 | Already in wishlist |
| `PRODUCT_NOT_ACTIVE` | 422 | Product is not available |

---

## 6. 구현 순서

```text
1. ErrorCode 보강 (common)
2. build.gradle.kts 의존성 추가 (app, domain)
3. application.properties — jwt.secret, jwt.expiration-ms 추가
4. JwtTokenProvider (app/config)
5. JwtAuthenticationFilter (app/config)
6. SecurityConfig (app/config)
7. AuthService interface + DTOs (domain)
8. AuthServiceImpl + UserRepository (domain-nearpick)
9. AuthController (app)
10. ProductService interface + DTOs (domain)
11. ProductRepository (Haversine 쿼리 포함) + ProductServiceImpl (domain-nearpick)
12. ProductController (app)
13. WishlistService → ServiceImpl → Controller
14. ReservationService → ServiceImpl → Controller
15. FlashPurchaseService → ServiceImpl → Controller
16. MerchantService → ServiceImpl → Controller (Dashboard)
17. AdminService → ServiceImpl → Controller
18. ./gradlew build 통과 확인
```

---

## 7. 완료 기준 체크

- [ ] 의존성 추가 후 `./gradlew build` 성공
- [ ] JWT 토큰 생성·검증 동작
- [ ] 24개 엔드포인트 구현 완료
- [ ] 인증 없이 보호 경로 접근 시 401 반환
- [ ] 역할 불일치 시 403 반환
- [ ] H2 Console에서 데이터 생성·조회 확인
- [ ] 재고 부족 시 OUT_OF_STOCK 에러 반환

---

## 다음 단계

`/pdca do phase4-api` → 구현 시작
