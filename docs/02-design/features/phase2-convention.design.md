# [Design] Phase 2 — Coding Convention

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase2-convention |
| Phase | Design |
| 작성일 | 2026-02-23 |
| 참조 | `docs/01-plan/features/phase2-convention.plan.md` |

---

## 1. 멀티모듈 Gradle 설계

### 1.1 디렉토리 구조

```
near-pick/                              ← root project
├── settings.gradle.kts
├── build.gradle.kts                    ← 공통 설정만 (subprojects 블록)
├── app/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/nearpick/app/
├── common/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/nearpick/common/
├── domain/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/nearpick/domain/
└── domain-nearpick/
    ├── build.gradle.kts
    └── src/main/kotlin/com/nearpick/nearpick/
```

### 1.2 `settings.gradle.kts`

```kotlin
rootProject.name = "near-pick"

include(
    "app",
    "common",
    "domain",
    "domain-nearpick"
)
```

### 1.3 루트 `build.gradle.kts` (공통 설정)

```kotlin
plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    group = "com.nearpick"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
```

### 1.4 `common/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter")  // ApplicationContext 최소 의존
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }
```

### 1.5 `domain/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // ※ JPA 의존성 없음 — 순수 Kotlin
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }
```

### 1.6 `domain-nearpick/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }
```

### 1.7 `app/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
    runtimeOnly(project(":domain-nearpick"))   // ← 컴파일 불가, Bean 탐색만
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }
```

---

## 2. 모듈별 패키지 구조

### `common`
```
com.nearpick.common/
├── exception/
│   ├── BusinessException.kt         # RuntimeException 상속 기반 예외
│   └── ErrorCode.kt                 # 에러 코드 enum
└── response/
    └── ApiResponse.kt               # 공통 API 응답 래퍼
```

### `domain`
```
com.nearpick.domain/
└── {도메인명}/                       # ex: product, user, transaction
    ├── {Domain}Service.kt           # interface
    ├── model/
    │   └── {Domain}.kt              # data class (순수 도메인 모델)
    └── dto/
        ├── request/
        │   └── {Action}{Domain}Request.kt
        └── response/
            └── {Domain}Response.kt
```

### `domain-nearpick`
```
com.nearpick.nearpick/
└── {도메인명}/
    ├── {Domain}ServiceImpl.kt       # implements {Domain}Service
    ├── {Domain}Repository.kt        # : JpaRepository<{Domain}Entity, Long>
    ├── {Domain}Entity.kt            # @Entity
    └── {Domain}Mapper.kt            # Entity ↔ domain model 변환
```

### `app`
```
com.nearpick.app/
├── NearPickApplication.kt           # @SpringBootApplication
├── config/
│   └── SecurityConfig.kt           # (Phase 7)
└── {도메인명}/
    └── {Domain}Controller.kt        # @RestController
```

---

## 3. Phase 1 코드 이전 계획

현재 단일 모듈에 있는 Phase 1 코드를 새 구조로 이전한다.

| 현재 위치 | 이전 대상 | 이전 후 모듈 |
|-----------|----------|-------------|
| `src/.../domain/user/entity/` | `User`, `ConsumerProfile`, `MerchantProfile`, `AdminProfile` | `domain-nearpick` |
| `src/.../domain/user/enums/` | `UserRole`, `UserStatus`, `AdminLevel` | `domain-nearpick` |
| `src/.../domain/product/entity/` | `Product`, `PopularityScore` | `domain-nearpick` |
| `src/.../domain/product/enums/` | `ProductType`, `ProductStatus` | `domain-nearpick` |
| `src/.../domain/transaction/entity/` | `Wishlist`, `Reservation`, `FlashPurchase` | `domain-nearpick` |
| `src/.../domain/transaction/enums/` | `ReservationStatus`, `FlashPurchaseStatus` | `domain-nearpick` |
| `src/.../NearPickApplication.kt` | 메인 클래스 | `app` |

> **이전 후 패키지명 변경:**
> `com.nearpick.app.domain.*` → `com.nearpick.nearpick.*`

---

## 4. 코드 예시 (product 도메인)

### `domain` 모듈 — 순수 모델 및 인터페이스

```kotlin
// domain: com/nearpick/domain/product/model/Product.kt
data class Product(
    val id: Long,
    val merchantId: Long,
    val title: String,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
)

// domain: com/nearpick/domain/product/ProductService.kt
interface ProductService {
    fun create(request: CreateProductRequest): ProductResponse
    fun findNearby(lat: Double, lng: Double, radiusKm: Double): List<ProductResponse>
    fun findById(id: Long): ProductResponse
}
```

### `domain-nearpick` 모듈 — 구현체

```kotlin
// domain-nearpick: com/nearpick/nearpick/product/ProductServiceImpl.kt
@Service
@Transactional
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val productMapper: ProductMapper,
) : ProductService {
    override fun create(request: CreateProductRequest): ProductResponse {
        val entity = productMapper.toEntity(request)
        return productMapper.toResponse(productRepository.save(entity))
    }
}

// domain-nearpick: com/nearpick/nearpick/product/ProductRepository.kt
interface ProductRepository : JpaRepository<ProductEntity, Long>

// domain-nearpick: com/nearpick/nearpick/product/ProductMapper.kt
@Component
class ProductMapper {
    fun toEntity(request: CreateProductRequest): ProductEntity { ... }
    fun toDomain(entity: ProductEntity): Product { ... }
    fun toResponse(entity: ProductEntity): ProductResponse { ... }
}
```

### `app` 모듈 — Controller

```kotlin
// app: com/nearpick/app/product/ProductController.kt
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,  // domain의 interface만 참조
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateProductRequest): ApiResponse<ProductResponse> =
        ApiResponse.success(productService.create(request))
}
```

---

## 5. `runtimeOnly` 강제 검증 방법

`app` 모듈에서 아래 코드가 **컴파일 에러**를 내면 올바르게 설정된 것:

```kotlin
// app 모듈에서 시도 시 컴파일 에러 발생해야 함
import com.nearpick.nearpick.product.ProductRepository  // ← 에러!
import com.nearpick.nearpick.product.ProductEntity      // ← 에러!
```

---

## 6. 완료 기준 체크

- [ ] `settings.gradle.kts` — 4개 모듈 등록
- [ ] 루트 `build.gradle.kts` — 공통 설정
- [ ] `common/build.gradle.kts`
- [ ] `domain/build.gradle.kts` — JPA 의존성 없음 확인
- [ ] `domain-nearpick/build.gradle.kts` — JPA 포함
- [ ] `app/build.gradle.kts` — `runtimeOnly(domain-nearpick)`
- [ ] Phase 1 Entity/Enum → `domain-nearpick` 이전
- [ ] `NearPickApplication` → `app` 이전
- [ ] `./gradlew build` 성공
- [ ] `CONVENTIONS.md` 생성

---

## 7. 다음 단계

`/pdca do phase2-convention` → Gradle 멀티모듈 구조 실제 구현
