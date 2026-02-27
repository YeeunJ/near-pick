# [Design] Phase 4.5 — API Quality (Swagger + Test)

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4.5-api-quality |
| Phase | Design |
| 작성일 | 2026-02-26 |
| 참조 | `docs/01-plan/features/phase4.5-api-quality.plan.md` |

---

## 1. Swagger / OpenAPI 설계

### 1-1. 의존성 추가

**`app/build.gradle.kts`**

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
```

> Spring Boot 4.x (Spring 7.x) 호환 버전 사용. 구현 시 최신 버전 확인 필수.

---

### 1-2. SwaggerConfig

**위치:** `app/src/main/kotlin/com/nearpick/app/config/SwaggerConfig.kt`

```kotlin
@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("NearPick API")
                .version("v1")
                .description("지역 기반 실시간 인기 상품 커머스 플랫폼 API")
        )
        .addSecurityItem(SecurityRequirement().addList("Bearer Authentication"))
        .components(
            Components().addSecuritySchemes(
                "Bearer Authentication",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("로그인 후 발급된 JWT 토큰을 입력하세요.")
            )
        )
}
```

---

### 1-3. SecurityConfig 수정 (local 환경 분리)

Swagger 경로를 `SecurityConfig`에 직접 추가하지 않고, `@Profile("local")` 전용 설정으로 분리한다.
prod 환경에서는 `LocalSwaggerSecurityConfig`가 로드되지 않으므로 Swagger 경로가 자동으로 보호된다.

**위치:** `app/src/main/kotlin/com/nearpick/app/config/LocalSwaggerSecurityConfig.kt`

```kotlin
@Configuration
@Profile("local")
class LocalSwaggerSecurityConfig {

    @Bean
    @Order(1)
    fun swaggerFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
        return http.build()
    }
}
```

> prod 환경에서는 `application-prod.properties`에 `springdoc.swagger-ui.enabled=false`와
> `springdoc.api-docs.enabled=false`를 추가로 설정하여 엔드포인트 자체를 비활성화한다.

---

### 1-4. Controller 어노테이션 설계

각 Controller에 `@Tag` (클래스 레벨) + `@Operation` (메서드 레벨)을 추가한다.

| Controller | @Tag name | 주요 엔드포인트 |
|------------|-----------|----------------|
| `AuthController` | `"Auth"` | signup/consumer, signup/merchant, login |
| `ProductController` | `"Products"` | nearby, detail, create, close, myProducts |
| `WishlistController` | `"Wishlists"` | toggle, list |
| `ReservationController` | `"Reservations"` | create, cancel, confirm, myList, merchantList |
| `FlashPurchaseController` | `"Flash Purchases"` | purchase, myList |
| `MerchantController` | `"Merchants"` | dashboard, profile |
| `AdminController` | `"Admin"` | userList, suspend, productList, reject |

**어노테이션 패턴 (예시: ProductController):**

```kotlin
@Tag(name = "Products", description = "상품 관련 API")
@RestController
@RequestMapping("/api/products")
class ProductController(...) {

    @Operation(
        summary = "주변 인기 상품 조회",
        description = "현재 위치(lat, lng) 기준 반경 내 활성 상품을 인기도순/거리순으로 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/nearby")
    fun getNearby(...): ...
}
```

**인증 필요 엔드포인트에는 Security 요구사항 명시:**

```kotlin
@SecurityRequirement(name = "Bearer Authentication")
@Operation(summary = "상품 등록")
@PostMapping
fun create(...): ...
```

---

## 2. JaCoCo 테스트 커버리지 설정

### 2-1. 루트 build.gradle.kts — 공통 적용

```kotlin
subprojects {
    // ... 기존 설정 유지 ...

    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required = true
            html.required = true
        }
    }
}
```

### 2-2. 모듈별 커버리지 제외 및 목표

**`app/build.gradle.kts` — Controller + Config 검증, Entity·Repository 제외**

```kotlin
tasks.jacocoTestReport {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("**/NearPickApplication*")
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { minimum = "0.60".toBigDecimal() }
        }
    }
}
```

**`domain/build.gradle.kts` — Value Object 90% 목표**

```kotlin
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            // dto, enum 제외하고 model만 90%
            includes = listOf("com.nearpick.domain.model.*")
            limit { minimum = "0.90".toBigDecimal() }
        }
    }
}
```

**`domain-nearpick/build.gradle.kts` — ServiceImpl 80% 목표, Entity·Repository·Mapper 제외**

```kotlin
tasks.jacocoTestReport {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/entity/**",
                "**/repository/**",
                "**/mapper/**",
                "**/JpaConfig*",
            )
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            includes = listOf("com.nearpick.nearpick.*.service.*")
            limit { minimum = "0.80".toBigDecimal() }
        }
    }
}
```

---

## 3. 테스트 파일 구조

```
app/src/test/kotlin/com/nearpick/app/
├── NearPickApplicationTests.kt                    # 통합: 컨텍스트 로딩
└── controller/
    ├── AuthControllerTest.kt
    ├── ProductControllerTest.kt
    ├── WishlistControllerTest.kt
    ├── ReservationControllerTest.kt
    ├── FlashPurchaseControllerTest.kt
    ├── MerchantControllerTest.kt
    └── AdminControllerTest.kt

domain/src/test/kotlin/com/nearpick/domain/
└── model/
    ├── EmailTest.kt
    ├── PasswordTest.kt
    ├── LocationTest.kt
    └── BusinessRegNoTest.kt

domain-nearpick/src/test/kotlin/com/nearpick/nearpick/
├── auth/service/AuthServiceImplTest.kt
├── transaction/service/FlashPurchaseServiceImplTest.kt
├── transaction/service/ReservationServiceImplTest.kt
└── user/service/MerchantServiceImplTest.kt
```

---

## 4. 테스트 설정 파일

### 4-1. application-test.properties

**위치:** `app/src/test/resources/application-test.properties`

> Spring Boot 4.x에서 `@WebMvcTest`가 제거되어 H2 in-memory DB를 사용할 수 없다.
> 테스트 환경도 로컬 MySQL(`nearpick_test` DB)에 직접 연결하고, Flyway는 비활성화한다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nearpick_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Flyway 비활성화 — 테스트 환경은 Hibernate create-drop으로 스키마 관리
spring.flyway.enabled=false

# JWT 테스트 시크릿 (32바이트 이상, 테스트 전용)
jwt.secret=test-secret-key-for-testing-only-must-be-long-enough
jwt.expiration-ms=3600000
```

---

## 5. Controller 테스트 설계

### 공통 설정

> Spring Boot 4.x에서 `@WebMvcTest`가 완전히 제거되었다.
> `@SpringBootTest(webEnvironment=MOCK)` + `MockMvcBuilders.webAppContextSetup`을 사용한다.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FooControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var fooService: FooService
}
```

- **인증 불필요 엔드포인트**: 그대로 요청
- **인증 필요 엔드포인트**: `@WithMockUser(roles = ["CONSUMER"])` 또는 `["MERCHANT"]`
- **`@AuthenticationPrincipal userId: Long`**: SecurityContext에 `UsernamePasswordAuthenticationToken(1L, ...)` 직접 설정

---

### 5-1. AuthControllerTest

| 메서드 | 시나리오 | 기대 결과 |
|--------|----------|-----------|
| `signupConsumer` | 정상 요청 | 201 Created |
| `signupConsumer` | 빈 email | 400 Bad Request |
| `signupMerchant` | 정상 요청 | 201 Created |
| `login` | 정상 요청 | 200 OK, accessToken 포함 |
| `login` | 비밀번호 오류 → service에서 예외 | 401 Unauthorized |

---

### 5-2. ProductControllerTest

| 메서드 | 시나리오 | 기대 결과 |
|--------|----------|-----------|
| `getNearby` | 정상 요청 (인증 불필요) | 200 OK |
| `getDetail` | 정상 요청 (인증 불필요) | 200 OK |
| `create` | MERCHANT 권한 + 정상 요청 | 201 Created |
| `create` | 인증 없음 | 403 Forbidden |
| `close` | MERCHANT 권한 + 정상 요청 | 200 OK |

---

### 5-3. FlashPurchaseControllerTest

| 메서드 | 시나리오 | 기대 결과 |
|--------|----------|-----------|
| `purchase` | CONSUMER 권한 + 정상 요청 | 201 Created |
| `purchase` | 인증 없음 | 403 Forbidden |
| `getMyPurchases` | CONSUMER 권한 + 정상 요청 | 200 OK |

---

### 5-4. ReservationControllerTest

| 메서드 | 시나리오 | 기대 결과 |
|--------|----------|-----------|
| `create` | CONSUMER 권한 + 정상 요청 | 201 Created |
| `cancel` | CONSUMER 권한 + 정상 요청 | 200 OK |
| `confirm` | MERCHANT 권한 + 정상 요청 | 200 OK |
| `getMyReservations` | CONSUMER 권한 | 200 OK |

---

### 5-5. WishlistControllerTest / MerchantControllerTest / AdminControllerTest

각 1~2개 주요 엔드포인트 정상/인증 실패 케이스 커버

---

## 6. Service 테스트 설계

### 6-1. AuthServiceImplTest

```kotlin
@ExtendWith(MockitoExtension::class)
class AuthServiceImplTest {
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var consumerProfileRepository: ConsumerProfileRepository
    @Mock lateinit var merchantProfileRepository: MerchantProfileRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @InjectMocks lateinit var authService: AuthServiceImpl
}
```

| 테스트 메서드 | 시나리오 |
|---------------|----------|
| `signupConsumer - 정상` | userRepository 저장 성공 → SignupResponse 반환 |
| `signupConsumer - 중복 이메일` | existsByEmail = true → DUPLICATE_EMAIL 예외 |
| `login - 정상` | 사용자 존재 + 비밀번호 일치 → LoginResult 반환 |
| `login - 사용자 없음` | findByEmail = null → INVALID_CREDENTIALS 예외 |
| `login - 비밀번호 불일치` | passwordEncoder.matches = false → INVALID_CREDENTIALS 예외 |
| `login - 정지된 계정` | status = SUSPENDED → ACCOUNT_SUSPENDED 예외 |

---

### 6-2. FlashPurchaseServiceImplTest

```kotlin
@ExtendWith(MockitoExtension::class)
class FlashPurchaseServiceImplTest {
    @Mock lateinit var flashPurchaseRepository: FlashPurchaseRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var productRepository: ProductRepository
    @InjectMocks lateinit var flashPurchaseService: FlashPurchaseServiceImpl
}
```

| 테스트 메서드 | 시나리오 |
|---------------|----------|
| `purchase - 정상` | stock 충분 + ACTIVE → 재고 차감 후 FlashPurchaseStatusResponse 반환 |
| `purchase - 재고 부족` | stock < quantity → OUT_OF_STOCK 예외 |
| `purchase - 비활성 상품` | status != ACTIVE → PRODUCT_NOT_ACTIVE 예외 |
| `purchase - 상품 없음` | findByIdWithLock = null → PRODUCT_NOT_FOUND 예외 |
| `getMyPurchases - 정상` | Page 반환 확인 |

---

### 6-3. ReservationServiceImplTest

| 테스트 메서드 | 시나리오 |
|---------------|----------|
| `create - 정상` | ACTIVE 상품 + 유효 사용자 → ReservationStatusResponse 반환 |
| `create - 비활성 상품` | status != ACTIVE → PRODUCT_NOT_ACTIVE 예외 |
| `cancel - 정상` | 본인 예약 + PENDING → CANCELLED 상태로 변경 |
| `cancel - 타인 예약` | userId 불일치 → FORBIDDEN 예외 |
| `cancel - PENDING 아님` | status != PENDING → RESERVATION_CANNOT_BE_CANCELLED 예외 |
| `confirm - 정상` | 해당 소상공인 예약 + PENDING → CONFIRMED 상태로 변경 |
| `confirm - 권한 없음` | merchantId 불일치 → FORBIDDEN 예외 |

---

### 6-4. MerchantServiceImplTest

| 테스트 메서드 | 시나리오 |
|---------------|----------|
| `getDashboard - 정상` | merchant 조회 → products + wishlist 집계 반환 |
| `getDashboard - merchant 없음` | findById 없음 → USER_NOT_FOUND 예외 |
| `getProfile - 정상` | MerchantProfileResponse 반환 |

---

## 7. Value Object 테스트 설계

### 7-1. EmailTest

| 테스트 | 검증 포인트 |
|--------|-------------|
| 유효한 이메일 생성 | 예외 없이 생성됨 |
| 공백 이메일 → 예외 | `IllegalArgumentException` 발생 |
| 255자 초과 → 예외 | `IllegalArgumentException` 발생 |
| @ 없는 형식 → 예외 | `IllegalArgumentException` 발생 |
| `masked()` | `"ab**@domain.com"` 형식 반환 |
| `localPart()` | @ 이전 문자열 반환 |

### 7-2. PasswordTest

| 테스트 | 검증 포인트 |
|--------|-------------|
| 유효한 비밀번호 | 예외 없이 생성됨 |
| 7자 이하 → 예외 | `IllegalArgumentException` |
| 숫자 없음 → 예외 | `IllegalArgumentException` |
| 문자 없음 → 예외 | `IllegalArgumentException` |

### 7-3. LocationTest

| 테스트 | 검증 포인트 |
|--------|-------------|
| 유효한 위도/경도 | 예외 없이 생성됨 |
| 위도 90 초과 → 예외 | `IllegalArgumentException` |
| 위도 -90 미만 → 예외 | `IllegalArgumentException` |
| 경도 180 초과 → 예외 | `IllegalArgumentException` |
| 경계값 (90.0, 180.0) | 예외 없이 생성됨 |

### 7-4. BusinessRegNoTest

| 테스트 | 검증 포인트 |
|--------|-------------|
| 유효한 형식 `123-45-67890` | 예외 없이 생성됨 |
| 공백 → 예외 | `IllegalArgumentException` |
| 형식 불일치 (하이픈 없음) → 예외 | `IllegalArgumentException` |
| 자릿수 오류 → 예외 | `IllegalArgumentException` |

---

## 8. 통합 테스트 설계

**위치:** `app/src/test/kotlin/com/nearpick/app/NearPickApplicationTests.kt`

```kotlin
@SpringBootTest
@ActiveProfiles("test")
class NearPickApplicationTests {

    @Test
    fun `애플리케이션 컨텍스트가 정상 로딩된다`() {
        // Spring context loads without error
    }
}
```

- MySQL test DB 사용 (`nearpick_test` DB, `application-test.properties`)
- 목적: Bean 설정 충돌, JPA 설정 오류 감지
- DB 쿼리 검증은 이 테스트의 범위 밖

---

## 9. 구현 순서

```
[Swagger]
1. app/build.gradle.kts — springdoc 의존성 추가
2. SwaggerConfig 작성 (OpenAPI Bean + JWT SecurityScheme)
3. SecurityConfig — Swagger 경로 permitAll 추가
4. Controller 7개 — @Tag / @Operation / @SecurityRequirement 어노테이션 추가

[JaCoCo]
5. build.gradle.kts (루트) — jacoco plugin + 기본 설정
6. 모듈별 jacocoTestCoverageVerification 목표 설정

[Test Properties]
7. app/src/test/resources/application-test.properties 생성

[Value Object 테스트]
8. domain/ 모듈 — EmailTest, PasswordTest, LocationTest, BusinessRegNoTest

[Service 테스트]
9. domain-nearpick/ — AuthServiceImplTest
10. domain-nearpick/ — FlashPurchaseServiceImplTest
11. domain-nearpick/ — ReservationServiceImplTest
12. domain-nearpick/ — MerchantServiceImplTest

[Controller 테스트]
13. app/ — AuthControllerTest
14. app/ — ProductControllerTest
15. app/ — FlashPurchaseControllerTest, ReservationControllerTest
16. app/ — WishlistControllerTest, MerchantControllerTest, AdminControllerTest

[통합 테스트]
17. NearPickApplicationTests 업데이트

[검증]
18. ./gradlew build (전체 테스트 + JaCoCo)
19. ./gradlew jacocoTestReport — 커버리지 리포트 확인
```

---

## 10. 리스크 및 대응

| 리스크 | 대응 |
|--------|------|
| springdoc-openapi 3.x Spring Boot 4.x 호환 미확인 | 구현 시작 전 `./gradlew dependencies`로 의존성 해결 확인 |
| `@AuthenticationPrincipal userId: Long` Controller 테스트 설정 복잡 | `SecurityContext` 직접 설정 또는 `@WithSecurityContext` 커스텀 어노테이션 |
| Spring Boot 4.x `@WebMvcTest` 제거 | `@SpringBootTest(webEnvironment=MOCK)` + `MockMvcBuilders.webAppContextSetup` 사용 |
| MySQL test DB 미존재 | `nearpick_test` DB 사전 생성 필수 (`CREATE DATABASE nearpick_test`) |
| JaCoCo + Kotlin inline value class 커버리지 측정 이슈 | Value Object 커버리지 제외 설정으로 대응 (목표 충족 어려울 경우) |

---

## 11. DB 환경 분리 — Flyway + ddl-auto

### 11-1. 의존성 추가

**`app/build.gradle.kts`**

```kotlin
implementation("org.springframework.boot:spring-boot-flyway")  // Spring Boot 4.x: Flyway auto-config 별도 모듈
implementation("org.flywaydb:flyway-core")
runtimeOnly("org.flywaydb:flyway-mysql")
```

> Spring Boot 4.x부터 Flyway auto-configuration이 `spring-boot-autoconfigure`에서 제거되어
> `spring-boot-flyway` 별도 모듈로 분리되었다. 반드시 명시 추가 필요.

---

### 11-2. Migration 파일

**`app/src/main/resources/db/migration/V1__init_schema.sql`**

전체 스키마 DDL: `users`, `consumer_profiles`, `merchant_profiles`, `admin_profiles`, `products`,
`popularity_scores`, `wishlists`, `reservations`, `flash_purchases` 8개 테이블.

> `baseline-version=1`로 설정하므로 V1은 baseline으로 표시되어 실행되지 않는다. V2부터 순차 적용.

**`app/src/main/resources/db/testdata/V2__insert_dummy_data.sql`**

local 개발용 더미 데이터: 사용자 6명 (password: `test1234`, BCrypt 해시), 상품 5개,
wishlist / reservation / flash_purchase 포함. `INSERT IGNORE`로 멱등성 보장.

---

### 11-3. 환경별 properties 설정

**`application-local.properties`** — 스키마 validate, Flyway 활성화 (migration + testdata)

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration,classpath:db/testdata
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

**`application-test.properties`** — ddl-auto=create-drop, Flyway 비활성화

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
```

> 테스트 환경은 Hibernate `create-drop`으로 스키마를 직접 관리하므로
> Flyway가 실행되면 충돌이 발생한다.

**`application-prod.properties.example`** — migration 전용, testdata 제외, Swagger 비활성화

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

---

### 11-4. 구현 순서

```
1. app/build.gradle.kts — spring-boot-flyway, flyway-core, flyway-mysql 추가
2. db/migration/V1__init_schema.sql — 전체 스키마 DDL 작성
3. db/testdata/V2__insert_dummy_data.sql — 더미 데이터 작성 (BCrypt 해시 포함)
4. application-local.properties — ddl-auto=validate + Flyway 설정
5. application-test.properties — spring.flyway.enabled=false 추가
6. application-prod.properties.example — migration 전용 + Swagger 비활성화
7. ./gradlew :app:bootRun — Flyway baseline + migration 로그 확인
```
