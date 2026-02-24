# 모듈 구조 가이드

NearPick은 4개의 Gradle 서브모듈로 구성된 멀티모듈 프로젝트입니다.

---

## 전체 구조

```
near-pick/                        ← 루트 프로젝트 (빌드 설정만)
├── app/                          ← 웹 진입점
├── common/                       ← 공통 기반
├── domain/                       ← 비즈니스 계약
└── domain-nearpick/              ← NearPick 구현체
```

---

## 의존성 흐름

```
app ──(compile)──────> domain ──(compile)──> common
 └──(runtimeOnly)──> domain-nearpick ──(compile)──> domain
                                      └──(compile)──> common
```

### `runtimeOnly`가 의미하는 것

`app`이 `domain-nearpick`을 `runtimeOnly`로 선언하면:

- `app` 코드에서 `domain-nearpick` 클래스를 `import`하면 **컴파일 에러** 발생
- Controller에서 Repository·Entity 직접 주입 **불가** — Service 인터페이스를 통해서만 접근
- Spring Boot는 런타임에 `domain-nearpick`의 Bean을 정상 탐색·주입

```kotlin
// app 모듈에서 이런 코드는 컴파일 에러!
import com.nearpick.nearpick.product.ProductRepository  // ← 에러
import com.nearpick.nearpick.product.ProductEntity      // ← 에러

// 올바른 방법: domain의 인터페이스만 참조
import com.nearpick.domain.product.ProductService       // ← OK
```

---

## 모듈별 상세

### `app` — 웹 진입점

**패키지**: `com.nearpick.app`

**역할**: HTTP 요청 수신 → Service 호출 → 응답 반환

**포함 요소**:
```
com.nearpick.app/
├── NearPickApplication.kt        # @SpringBootApplication
├── config/                       # SecurityConfig 등 (Phase 7)
└── {domain}/
    └── {Domain}Controller.kt     # @RestController
```

**의존성**:
- `implementation(project(":domain"))` — Service 인터페이스 참조
- `implementation(project(":common"))` — ApiResponse, Exception 사용
- `runtimeOnly(project(":domain-nearpick"))` — Bean 탐색만, 직접 import 불가
- `spring-boot-starter-web`

---

### `common` — 공통 기반

**패키지**: `com.nearpick.common`

**역할**: 프로젝트 전체에서 공유하는 유틸리티. 다른 모듈에 **의존하지 않음**.

**포함 요소**:
```
com.nearpick.common/
├── exception/
│   ├── BusinessException.kt      # RuntimeException 기반 커스텀 예외
│   └── ErrorCode.kt              # 에러 코드 enum
└── response/
    └── ApiResponse.kt            # 공통 API 응답 래퍼
```

**의존성**: 없음 (독립 모듈)

---

### `domain` — 비즈니스 계약

**패키지**: `com.nearpick.domain`

**역할**: JPA 없는 순수 비즈니스 계층. `app`이 이 모듈만 참조해도 기능 구현 가능.

**포함 요소**:
```
com.nearpick.domain/
└── {domain}/                     # ex: product, user, transaction
    ├── {Domain}Service.kt        # interface (비즈니스 계약)
    ├── {DomainEnum}.kt           # Enum (순수 도메인 타입)
    ├── model/
    │   └── {Domain}.kt           # data class (순수 도메인 모델)
    └── dto/
        ├── request/
        │   └── {Action}{Domain}Request.kt
        └── response/
            └── {Domain}Response.kt
```

**현재 위치한 파일들**: UserRole, UserStatus, AdminLevel, ProductType, ProductStatus,
ReservationStatus, FlashPurchaseStatus (Enum 7개)

**의존성**:
- `implementation(project(":common"))`
- JPA 의존성 **없음** — 순수 Kotlin/Java만 허용

---

### `domain-nearpick` — NearPick 구현체

**패키지**: `com.nearpick.nearpick`

**역할**: `domain`의 Service 인터페이스를 구현. JPA Entity와 비즈니스 로직이 여기에 위치.

**포함 요소**:
```
com.nearpick.nearpick/
└── {domain}/
    ├── {Domain}ServiceImpl.kt    # implements {Domain}Service
    ├── {Domain}Repository.kt     # : JpaRepository<{Domain}Entity, Long>
    ├── {Domain}Entity.kt         # @Entity
    └── {Domain}Mapper.kt         # Entity ↔ domain model 변환
```

**현재 위치한 파일들**: UserEntity, ConsumerProfileEntity, MerchantProfileEntity,
AdminProfileEntity, ProductEntity, PopularityScoreEntity, WishlistEntity,
ReservationEntity, FlashPurchaseEntity (Entity 9개)

**의존성**:
- `implementation(project(":domain"))`
- `implementation(project(":common"))`
- `spring-boot-starter-data-jpa`
- `runtimeOnly("org.postgresql:postgresql")`
- `runtimeOnly("com.h2database:h2")`

---

## 계층 간 데이터 흐름

```
HTTP Request
    ↓
Controller (app)
    ↓  Request DTO
Service interface (domain)
    ↓  implements
ServiceImpl (domain-nearpick)
    ↓  Entity
Repository (domain-nearpick)
    ↓
DB
    ↑
Repository → ServiceImpl
    ↑  domain model / Response DTO
Service → Controller
    ↑
HTTP Response
```

---

## 새 도메인 기능 추가 체크리스트

1. `domain` 모듈에 Service 인터페이스 + DTO + (필요 시) Enum 추가
2. `domain-nearpick` 모듈에 Entity + Repository + ServiceImpl + Mapper 추가
3. `app` 모듈에 Controller 추가
4. `common` 모듈의 ErrorCode에 필요한 에러 코드 추가
