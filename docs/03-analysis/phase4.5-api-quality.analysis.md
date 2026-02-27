# Phase 4.5 API Quality Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: NearPick
> **Version**: 0.0.1-SNAPSHOT
> **Analyst**: Claude Code (gap-detector)
> **Date**: 2026-02-27
> **Design Doc**: [phase4.5-api-quality.design.md](../02-design/features/phase4.5-api-quality.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Phase 4.5 API Quality (Swagger + Test) 설계 문서 대비 실제 구현 코드의 일치율 검증.
Swagger/OpenAPI 설정, JaCoCo 커버리지 설정, Controller/Service/VO 테스트 시나리오를 항목별로 비교한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/phase4.5-api-quality.design.md`
- **Implementation Paths**:
  - `app/src/main/kotlin/com/nearpick/app/config/` (Swagger, Security)
  - `build.gradle.kts` (root), `app/build.gradle.kts`, `domain/build.gradle.kts`, `domain-nearpick/build.gradle.kts` (JaCoCo)
  - `app/src/test/kotlin/com/nearpick/app/controller/` (Controller Tests)
  - `domain/src/test/kotlin/com/nearpick/domain/model/` (VO Tests)
  - `domain-nearpick/src/test/kotlin/com/nearpick/nearpick/` (Service Tests)
  - `app/src/test/resources/application-test.properties` (Test Properties)
- **Analysis Date**: 2026-02-27

### 1.3 Known Intentional Deviations

다음 항목은 구현 과정에서 의도적으로 결정한 변경이므로 Gap으로 산정하지 않는다.

| Deviation | Design | Implementation | Reason |
|-----------|--------|----------------|--------|
| Test DB | H2 in-memory | MySQL (nearpick_test) | Spring Boot 4.x @WebMvcTest 제거로 @SpringBootTest 사용 시 MySQL 필요 |
| Controller Test Base | @WebMvcTest | @SpringBootTest(webEnvironment=MOCK) | Spring Boot 4.x breaking change |
| Swagger Security | SecurityConfig.permitAll | LocalSwaggerSecurityConfig (@Profile("local")) | local/prod 환경 분리 강화 |

---

## 2. Gap Analysis (Design vs Implementation)

### 2.1 Swagger / OpenAPI (Design Section 1)

#### 2.1.1 Dependencies

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| springdoc-openapi | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0` | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0` | Match |

#### 2.1.2 SwaggerConfig

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| Location | `app/.../config/SwaggerConfig.kt` | `app/.../config/SwaggerConfig.kt` | Match |
| @Configuration | O | O | Match |
| OpenAPI Bean | O | O | Match |
| Info.title | "NearPick API" | "NearPick API" | Match |
| Info.version | "v1" | "v1" | Match |
| Info.description | O | O | Match |
| SecurityRequirement | "Bearer Authentication" | "Bearer Authentication" | Match |
| SecurityScheme type | HTTP / bearer / JWT | HTTP / bearer / JWT | Match |
| SecurityScheme description | O | O | Match |

**Score: 9/9 (100%)**

#### 2.1.3 SecurityConfig - Swagger Paths

| Item | Design | Implementation | Status | Notes |
|------|--------|----------------|--------|-------|
| Swagger permitAll | SecurityConfig에 직접 추가 | LocalSwaggerSecurityConfig (@Profile("local"), @Order(1)) | Intentional Deviation | local/prod 분리, 보안 강화 |

**Score: 1/1 (100%, intentional deviation)**

#### 2.1.4 Controller Annotations

| Controller | Design @Tag | Impl @Tag | @Operation | @SecurityRequirement | Status |
|------------|-------------|-----------|------------|---------------------|--------|
| AuthController | "Auth" | "Auth" | 3 methods | N/A (public) | Match |
| ProductController | "Products" | "Products" | 5 methods | create, close, myProducts | Match |
| WishlistController | "Wishlists" | "Wishlists" | 3 methods | class-level | Match |
| ReservationController | "Reservations" | "Reservations" | 5 methods | class-level | Match |
| FlashPurchaseController | "Flash Purchases" | "Flash Purchases" | 2 methods | class-level | Match |
| MerchantController | "Merchants" | "Merchants" | 2 methods | class-level | Match |
| AdminController | "Admin" | "Admin" | 6 methods | class-level | Match |

**Score: 7/7 (100%)**

**Swagger Total: 17/17 items matched (100%)**

---

### 2.2 JaCoCo Configuration (Design Section 2)

#### 2.2.1 Root build.gradle.kts

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| `apply(plugin = "jacoco")` in subprojects | O | O | Match |
| toolVersion = "0.8.12" | O | O | Match |
| JacocoReport depends on Test | O | O | Match |
| XML report required | O | O | Match |
| HTML report required | O | O | Match |

#### 2.2.2 app/build.gradle.kts

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| jacocoTestReport exclude NearPickApplication | O | O | Match |
| coverageVerification minimum 0.60 | O | O | Match |

#### 2.2.3 domain/build.gradle.kts

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| includes com.nearpick.domain.model.* | O | O | Match |
| minimum 0.90 | O | O | Match |

#### 2.2.4 domain-nearpick/build.gradle.kts

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| jacocoTestReport exclude entity/** | O | O | Match |
| jacocoTestReport exclude repository/** | O | O | Match |
| jacocoTestReport exclude mapper/** | O | O | Match |
| jacocoTestReport exclude JpaConfig* | O | O | Match |
| includes com.nearpick.nearpick.*.service.* | O | O | Match |
| minimum 0.80 | O | O | Match |

**JaCoCo Total: 15/15 items matched (100%)**

---

### 2.3 Test File Structure (Design Section 3)

| Design File | Implementation File | Status |
|-------------|---------------------|--------|
| `app/.../NearPickApplicationTests.kt` | Exists | Match |
| `app/.../controller/AuthControllerTest.kt` | Exists | Match |
| `app/.../controller/ProductControllerTest.kt` | Exists | Match |
| `app/.../controller/WishlistControllerTest.kt` | Exists | Match |
| `app/.../controller/ReservationControllerTest.kt` | Exists | Match |
| `app/.../controller/FlashPurchaseControllerTest.kt` | Exists | Match |
| `app/.../controller/MerchantControllerTest.kt` | Exists | Match |
| `app/.../controller/AdminControllerTest.kt` | Exists | Match |
| `domain/.../model/EmailTest.kt` | Exists | Match |
| `domain/.../model/PasswordTest.kt` | Exists | Match |
| `domain/.../model/LocationTest.kt` | Exists | Match |
| `domain/.../model/BusinessRegNoTest.kt` | Exists | Match |
| `domain-nearpick/.../auth/service/AuthServiceImplTest.kt` | Exists | Match |
| `domain-nearpick/.../transaction/service/FlashPurchaseServiceImplTest.kt` | Exists | Match |
| `domain-nearpick/.../transaction/service/ReservationServiceImplTest.kt` | Exists | Match |
| `domain-nearpick/.../user/service/MerchantServiceImplTest.kt` | Exists | Match |

**Test File Structure Total: 16/16 files exist (100%)**

---

### 2.4 Test Properties (Design Section 4)

| Item | Design | Implementation | Status | Notes |
|------|--------|----------------|--------|-------|
| Location | `app/src/test/resources/application-test.properties` | Exists | Match | |
| DB Type | H2 in-memory | MySQL (nearpick_test) | Intentional Deviation | Spring Boot 4.x |
| DDL Auto | create-drop | create-drop | Match | |
| JWT secret | test-secret-key... | test-secret-key... (different suffix) | Match | Both 32+ bytes |
| JWT expiration | 3600000 | 3600000 | Match | |
| show-sql | false | false | Match | |
| Flyway | Not mentioned | `spring.flyway.enabled=false` | Added | Flyway disable for test |

**Test Properties Total: 5/5 core items matched, 1 intentional deviation, 1 enhancement (100%)**

---

### 2.5 Controller Test Scenarios (Design Section 5)

#### 2.5.1 AuthControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| signupConsumer - 정상 요청 -> 201 | `POST auth-signup-consumer - 정상 요청 시 201을 반환한다` | Match |
| signupConsumer - 빈 email -> 400 | `POST auth-signup-consumer - 이메일 형식이 잘못되면 400을 반환한다` | Match |
| signupMerchant - 정상 요청 -> 201 | Not implemented | **Missing** |
| login - 정상 요청 -> 200 + accessToken | `POST auth-login - 정상 요청 시 200과 accessToken을 반환한다` | Match |
| login - 비밀번호 오류 -> 401 | `POST auth-login - 비밀번호 오류 시 서비스 예외가 GlobalExceptionHandler를 통해 처리된다` | Match |

**AuthControllerTest: 4/5 (80%)**

#### 2.5.2 ProductControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| getNearby - 인증 불필요 -> 200 | `GET api-products-nearby - 인증 없이 200을 반환한다` | Match |
| getDetail - 인증 불필요 -> 200 | `GET api-products-productId - 인증 없이 200을 반환한다` | Match |
| create - MERCHANT + 정상 -> 201 | `POST api-products - MERCHANT 권한으로 201을 반환한다` | Match |
| create - 인증 없음 -> 403 | `POST api-products - 인증 없으면 403을 반환한다` | Match |
| close - MERCHANT + 정상 -> 200 | Not implemented | **Missing** |

**ProductControllerTest: 4/5 (80%)**

#### 2.5.3 FlashPurchaseControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| purchase - CONSUMER + 정상 -> 201 | `POST api-flash-purchases - CONSUMER 권한으로 201을 반환한다` | Match |
| purchase - 인증 없음 -> 403 | `POST api-flash-purchases - 인증 없으면 403을 반환한다` | Match |
| getMyPurchases - CONSUMER + 정상 -> 200 | `GET api-flash-purchases-me - CONSUMER 권한으로 200을 반환한다` | Match |

**FlashPurchaseControllerTest: 3/3 (100%)**

#### 2.5.4 ReservationControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| create - CONSUMER + 정상 -> 201 | `POST api-reservations - CONSUMER 권한으로 201을 반환한다` | Match |
| cancel - CONSUMER + 정상 -> 200 | `PATCH api-reservations-id-cancel - CONSUMER 권한으로 200을 반환한다` | Match |
| confirm - MERCHANT + 정상 -> 200 | `PATCH api-reservations-id-confirm - MERCHANT 권한으로 200을 반환한다` | Match |
| getMyReservations - CONSUMER -> 200 | `GET api-reservations-me - CONSUMER 권한으로 200을 반환한다` | Match |

**ReservationControllerTest: 4/4 (100%)**

#### 2.5.5 WishlistControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 정상 + 인증 | `POST api-wishlists - CONSUMER 권한으로 201을 반환한다` | Match |
| 목록 조회 | `GET api-wishlists-me - CONSUMER 권한으로 200을 반환한다` | Match |
| 인증 실패 | `POST api-wishlists - 인증 없으면 403을 반환한다` | Match |

**WishlistControllerTest: 3/3 (100%)**

#### 2.5.6 MerchantControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| dashboard 정상 | `GET api-merchants-me-dashboard - MERCHANT 권한으로 200을 반환한다` | Match |
| profile 정상 | `GET api-merchants-me-profile - MERCHANT 권한으로 200을 반환한다` | Match |
| 인증 실패 | `GET api-merchants-me-dashboard - 인증 없으면 403을 반환한다` | Match |

**MerchantControllerTest: 3/3 (100%)**

#### 2.5.7 AdminControllerTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 사용자 목록 조회 정상 | `GET api-admin-users - ADMIN 권한으로 200을 반환한다` | Match |
| 인증 실패 | `GET api-admin-users - 인증 없으면 403을 반환한다` | Match |
| 상품 목록 조회 정상 | `GET api-admin-products - ADMIN 권한으로 200을 반환한다` | Match |

**AdminControllerTest: 3/3 (100%)**

**Controller Test Total: 24/26 scenarios (92.3%)**

---

### 2.6 Service Test Scenarios (Design Section 6)

#### 2.6.1 AuthServiceImplTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| signupConsumer - 정상 | `signupConsumer - 정상 요청 시 SignupResponse를 반환한다` | Match |
| signupConsumer - 중복 이메일 | `signupConsumer - 이메일 중복 시 DUPLICATE_EMAIL 예외를 던진다` | Match |
| login - 정상 | `login - 정상 요청 시 LoginResult를 반환한다` | Match |
| login - 사용자 없음 | `login - 존재하지 않는 이메일이면 INVALID_CREDENTIALS 예외를 던진다` | Match |
| login - 비밀번호 불일치 | `login - 비밀번호 불일치 시 INVALID_CREDENTIALS 예외를 던진다` | Match |
| login - 정지된 계정 | `login - 정지된 계정이면 ACCOUNT_SUSPENDED 예외를 던진다` | Match |
| (N/A) login - 탈퇴 계정 | `login - 탈퇴 계정이면 INVALID_CREDENTIALS 예외를 던진다` | **Added** |

**AuthServiceImplTest: 6/6 design scenarios + 1 enhancement (100%)**

#### 2.6.2 FlashPurchaseServiceImplTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| purchase - 정상 (재고 차감) | `purchase - 재고가 충분하면 재고를 차감하고 구매를 저장한다` | Match |
| purchase - 재고 부족 | `purchase - 재고가 부족하면 OUT_OF_STOCK 예외를 던진다` | Match |
| purchase - 비활성 상품 | `purchase - 상품이 비활성 상태이면 PRODUCT_NOT_ACTIVE 예외를 던진다` | Match |
| purchase - 상품 없음 | `purchase - 상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다` | Match |
| getMyPurchases - 정상 | Not implemented | **Missing** |
| (N/A) purchase - 사용자 없음 | `purchase - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다` | **Added** |

**FlashPurchaseServiceImplTest: 4/5 design scenarios + 1 enhancement (80%)**

#### 2.6.3 ReservationServiceImplTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| create - 정상 | `create - 정상 요청 시 ReservationStatusResponse를 반환한다` | Match |
| create - 비활성 상품 | `create - 비활성 상품이면 PRODUCT_NOT_ACTIVE 예외를 던진다` | Match |
| cancel - 정상 | `cancel - 본인 예약이고 PENDING 상태이면 CANCELLED로 변경된다` | Match |
| cancel - 타인 예약 | `cancel - 다른 사용자의 예약이면 FORBIDDEN 예외를 던진다` | Match |
| cancel - PENDING 아님 | `cancel - PENDING이 아닌 예약은 RESERVATION_CANNOT_BE_CANCELLED 예외를 던진다` | Match |
| confirm - 정상 | `confirm - 해당 소상공인의 예약이고 PENDING 상태이면 CONFIRMED로 변경된다` | Match |
| confirm - 권한 없음 | `confirm - 다른 소상공인이 확정 시도하면 FORBIDDEN 예외를 던진다` | Match |
| (N/A) confirm - PENDING 아님 | `confirm - PENDING이 아닌 예약은 RESERVATION_CANNOT_BE_CONFIRMED 예외를 던진다` | **Added** |

**ReservationServiceImplTest: 7/7 design scenarios + 1 enhancement (100%)**

#### 2.6.4 MerchantServiceImplTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| getDashboard - 정상 | `getDashboard - 정상 요청 시 MerchantDashboardResponse를 반환한다` | Match |
| getDashboard - merchant 없음 | `getDashboard - 존재하지 않는 merchantId이면 USER_NOT_FOUND 예외를 던진다` | Match |
| getProfile - 정상 | `getProfile - 정상 요청 시 MerchantProfileResponse를 반환한다` | Match |
| (N/A) getProfile - 없음 | `getProfile - 존재하지 않는 merchantId이면 USER_NOT_FOUND 예외를 던진다` | **Added** |

**MerchantServiceImplTest: 3/3 design scenarios + 1 enhancement (100%)**

**Service Test Total: 20/21 design scenarios (95.2%) + 4 enhancements**

---

### 2.7 Value Object Test Scenarios (Design Section 7)

#### 2.7.1 EmailTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 유효한 이메일 생성 | `유효한 이메일로 생성된다` | Match |
| 공백 이메일 -> 예외 | `공백 이메일은 예외를 던진다` | Match |
| 255자 초과 -> 예외 | `255자를 초과하는 이메일은 예외를 던진다` | Match |
| @ 없는 형식 -> 예외 | `@ 없는 형식은 예외를 던진다` | Match |
| masked() | `masked는 앞 2자리만 노출하고 나머지를 마스킹한다` | Match |
| localPart() | `localPart는 @ 이전 문자열을 반환한다` | Match |
| (N/A) 도메인 없는 형식 | `도메인 없는 형식은 예외를 던진다` | **Added** |
| (N/A) masked 2자 이하 | `atIndex가 2 이하인 경우 masked는 원본을 반환한다` | **Added** |
| (N/A) 경계값 255자 | `경계값 255자 이메일은 생성된다` | **Added** |

**EmailTest: 6/6 design scenarios + 3 enhancements (100%)**

#### 2.7.2 PasswordTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 유효한 비밀번호 | `유효한 비밀번호로 생성된다` | Match |
| 7자 이하 -> 예외 | `8자 미만이면 예외를 던진다` (same semantics) | Match |
| 숫자 없음 -> 예외 | `숫자가 없으면 예외를 던진다` | Match |
| 문자 없음 -> 예외 | `문자가 없으면 예외를 던진다` | Match |
| (N/A) 경계값 8자 | `정확히 8자인 경우 생성된다` | **Added** |

**PasswordTest: 4/4 design scenarios + 1 enhancement (100%)**

#### 2.7.3 LocationTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 유효한 위도/경도 | `유효한 위도와 경도로 생성된다` | Match |
| 위도 90 초과 -> 예외 | `위도 90 초과 시 예외를 던진다` | Match |
| 위도 -90 미만 -> 예외 | `위도 -90 미만 시 예외를 던진다` | Match |
| 경도 180 초과 -> 예외 | `경도 180 초과 시 예외를 던진다` | Match |
| 경계값 (90.0, 180.0) | `경계값 위도 90과 경도 180은 생성된다` | Match |
| (N/A) 경도 -180 미만 | `경도 -180 미만 시 예외를 던진다` | **Added** |
| (N/A) 경계값 (-90, -180) | `경계값 위도 -90과 경도 -180은 생성된다` | **Added** |

**LocationTest: 5/5 design scenarios + 2 enhancements (100%)**

#### 2.7.4 BusinessRegNoTest

| Design Scenario | Implementation | Status |
|----------------|----------------|--------|
| 유효한 형식 `123-45-67890` | `유효한 사업자등록번호로 생성된다` | Match |
| 공백 -> 예외 | `공백은 예외를 던진다` | Match |
| 형식 불일치 (하이픈 없음) | `하이픈 없는 형식은 예외를 던진다` | Match |
| 자릿수 오류 | `첫 번째 그룹 자릿수 오류는 예외를 던진다` + 2nd, 3rd group | Match (3 tests cover) |
| (N/A) 문자 포함 | `문자 포함 시 예외를 던진다` | **Added** |

**BusinessRegNoTest: 4/4 design scenarios + 1 enhancement (100%)**

**VO Test Total: 19/19 design scenarios (100%) + 7 enhancements**

---

### 2.8 Integration Test (Design Section 8)

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| @SpringBootTest | O | O | Match |
| @ActiveProfiles("test") | O | O | Match |
| Context loading test | O | O | Match |

**Integration Test: 3/3 (100%)**

---

### 2.9 Controller Test Common Setup (Design Section 5 - Common)

| Item | Design | Implementation | Status |
|------|--------|----------------|--------|
| Test annotation | @WebMvcTest | @SpringBootTest(webEnvironment=MOCK) | Intentional Deviation |
| MockMvc setup | @Autowired | MockMvcBuilders.webAppContextSetup | Intentional Deviation |
| Service mock | @MockitoBean | @MockitoBean | Match |
| Security integration | @Import(SecurityConfig...) | springSecurity() applied | Match (equivalent) |
| @WithMockUser approach | @WithMockUser | UsernamePasswordAuthenticationToken | Match (equivalent, more precise) |

**Common Setup: Equivalent implementation with Spring Boot 4.x adaptations**

---

## 3. Overall Match Rate Summary

### 3.1 Category Scores

| Category | Design Items | Matched | Missing | Added (Enhancement) | Match Rate |
|----------|:-----------:|:-------:|:-------:|:-------------------:|:----------:|
| Swagger/OpenAPI (S1) | 17 | 17 | 0 | 0 | 100% |
| JaCoCo (S2) | 15 | 15 | 0 | 0 | 100% |
| Test File Structure (S3) | 16 | 16 | 0 | 0 | 100% |
| Test Properties (S4) | 5 | 5 | 0 | 1 | 100% |
| Controller Tests (S5) | 26 | 24 | 2 | 0 | 92.3% |
| Service Tests (S6) | 21 | 20 | 1 | 4 | 95.2% |
| VO Tests (S7) | 19 | 19 | 0 | 7 | 100% |
| Integration Test (S8) | 3 | 3 | 0 | 0 | 100% |
| **Total** | **122** | **119** | **3** | **12** | **97.5%** |

### 3.2 Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 97.5% | Pass |
| Architecture Compliance | 100% | Pass |
| Convention Compliance | 100% | Pass |
| **Overall** | **97.5%** | **Pass** |

```
Overall Match Rate: 97.5%

  Match:              119 items (97.5%)
  Missing (Design O, Impl X): 3 items (2.5%)
  Added (Design X, Impl O):  12 items (enhancement)
```

---

## 4. Differences Found

### 4.1 Missing Features (Design O, Implementation X)

| # | Item | Design Location | Description | Impact |
|---|------|-----------------|-------------|--------|
| 1 | AuthControllerTest - signupMerchant 정상 | design.md:278 (S5-1) | `POST /auth/signup/merchant` 정상 요청 -> 201 테스트 미구현 | Low |
| 2 | ProductControllerTest - close 정상 | design.md:293 (S5-2) | `PATCH /api/products/{id}/close` MERCHANT 정상 -> 200 테스트 미구현 | Low |
| 3 | FlashPurchaseServiceImplTest - getMyPurchases | design.md:367 (S6-2) | `getMyPurchases - 정상 Page 반환 확인` 테스트 미구현 | Low |

### 4.2 Added Features (Design X, Implementation O)

| # | Item | Implementation Location | Description |
|---|------|------------------------|-------------|
| 1 | AuthServiceImplTest - 탈퇴 계정 | `domain-nearpick/.../AuthServiceImplTest.kt:131` | login 시 WITHDRAWN 상태 처리 테스트 추가 |
| 2 | FlashPurchaseServiceImplTest - 사용자 없음 | `domain-nearpick/.../FlashPurchaseServiceImplTest.kt:117` | USER_NOT_FOUND 예외 테스트 추가 |
| 3 | ReservationServiceImplTest - confirm PENDING 아님 | `domain-nearpick/.../ReservationServiceImplTest.kt:173` | RESERVATION_CANNOT_BE_CONFIRMED 예외 테스트 추가 |
| 4 | MerchantServiceImplTest - getProfile 없음 | `domain-nearpick/.../MerchantServiceImplTest.kt:98` | getProfile USER_NOT_FOUND 테스트 추가 |
| 5 | EmailTest - 도메인 없음, masked 경계, 255자 경계 | `domain/.../EmailTest.kt:33,49,55` | 3개 추가 경계값 테스트 |
| 6 | PasswordTest - 경계값 8자 | `domain/.../PasswordTest.kt:29` | 정확히 8자 경계값 테스트 |
| 7 | LocationTest - 경도 -180, 경계값 (-90,-180) | `domain/.../LocationTest.kt:33,43` | 음수 경계값 테스트 2개 추가 |
| 8 | BusinessRegNoTest - 문자 포함 | `domain/.../BusinessRegNoTest.kt:40` | 숫자 외 문자 포함 테스트 추가 |
| 9 | application-test.properties - Flyway disable | `app/src/test/resources/application-test.properties:23` | Flyway 비활성화 설정 추가 |
| 10 | WishlistController - POST/DELETE 분리 | `app/.../controller/WishlistController.kt` | Design의 toggle -> POST add + DELETE remove로 분리 |
| 11 | AdminController - 추가 엔드포인트 | `app/.../controller/AdminController.kt` | withdrawUser, forceCloseProduct, getProfile 추가 |
| 12 | LocalSwaggerSecurityConfig | `app/.../config/LocalSwaggerSecurityConfig.kt` | @Profile("local") 기반 Swagger 보안 분리 |

---

## 5. Architecture Compliance

### 5.1 Module Dependency Verification

| Module | Expected Dependencies | Actual Dependencies | Status |
|--------|----------------------|---------------------|--------|
| app (Controller) | domain, common | domain, common (+runtimeOnly domain-nearpick) | Pass |
| common | none | none | Pass |
| domain | common | common | Pass |
| domain-nearpick | domain, common | domain, common | Pass |

### 5.2 Test Framework Compliance

| Item | Design Pattern | Implementation | Status |
|------|---------------|----------------|--------|
| Controller Tests | @SpringBootTest + MockMvc + @MockitoBean | Correct | Pass |
| Service Tests | @ExtendWith(MockitoExtension) + @Mock + @InjectMocks | Correct | Pass |
| VO Tests | Plain JUnit 5 | Correct | Pass |
| Integration Test | @SpringBootTest + @ActiveProfiles("test") | Correct | Pass |

**Architecture Score: 100%**

---

## 6. Convention Compliance

### 6.1 Naming Convention

| Category | Convention | Compliance |
|----------|-----------|:----------:|
| Test Classes | PascalCase + Test suffix | 100% |
| Test Methods | Backtick Korean descriptive | 100% |
| Config Classes | PascalCase + Config suffix | 100% |
| Packages | lowercase dot-separated | 100% |

### 6.2 File Organization

| Item | Status |
|------|--------|
| Controller tests in `app/src/test/.../controller/` | Pass |
| Service tests in `domain-nearpick/src/test/.../` matching source structure | Pass |
| VO tests in `domain/src/test/.../model/` | Pass |
| Test properties in `app/src/test/resources/` | Pass |

**Convention Score: 100%**

---

## 7. Recommended Actions

### 7.1 Immediate Actions (optional, low impact)

| Priority | Item | Location | Description |
|----------|------|----------|-------------|
| Low | signupMerchant 테스트 추가 | `AuthControllerTest.kt` | 정상 요청 -> 201 시나리오 1개 추가 |
| Low | close 테스트 추가 | `ProductControllerTest.kt` | MERCHANT 권한 + 정상 -> 200 시나리오 1개 추가 |
| Low | getMyPurchases 테스트 추가 | `FlashPurchaseServiceImplTest.kt` | Page 반환 확인 시나리오 1개 추가 |

### 7.2 Design Document Updates Needed

다음 항목은 구현이 설계보다 더 풍부하므로 설계 문서에 반영하면 좋다.

- [ ] WishlistController의 POST/DELETE 분리 반영 (toggle -> add + remove)
- [ ] AdminController 추가 엔드포인트 (withdrawUser, forceCloseProduct, getProfile) 반영
- [ ] LocalSwaggerSecurityConfig (@Profile("local")) 방식 반영
- [ ] application-test.properties Flyway 비활성화 설정 반영
- [ ] 추가된 12개 enhancement 테스트 시나리오 반영

---

## 8. Conclusion

Phase 4.5 API Quality 설계 대비 구현 일치율은 **97.5%** 로 목표치(90%)를 초과 달성했다.

- **누락 항목 3건**: 모두 테스트 시나리오 누락(Low impact)이며 기능 구현 자체는 완료됨
- **추가 구현 12건**: 설계보다 더 풍부한 테스트 커버리지와 보안 강화 구현
- **의도적 변경 3건**: Spring Boot 4.x 호환성 및 환경 분리를 위한 합리적 변경

Match Rate >= 90% 조건을 충족하므로 Check 단계를 **통과(Pass)** 처리한다.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-02-27 | Initial gap analysis | Claude Code (gap-detector) |
