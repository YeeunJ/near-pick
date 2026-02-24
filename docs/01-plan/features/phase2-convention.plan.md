# [Plan] Phase 2 — Coding Convention

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase2-convention |
| Phase | Plan |
| 작성일 | 2026-02-23 |
| 프로젝트 | NearPick |
| 레벨 | Enterprise |
| 참조 | Phase 1 스키마 기반 |

---

## 1. 목표 (Objective)

NearPick 개발 전반에 걸쳐 일관된 코드 스타일을 확립한다.
AI와 개발자가 동일한 규칙으로 코드를 작성할 수 있도록 명확한 기준을 문서화한다.

---

## 2. 배경 (Background)

- **스택:** Spring Boot 4.0.3, Kotlin 2.2.21, Java 17
- **JPA:** Hibernate (Spring Data JPA)
- **빌드:** Gradle Kotlin DSL (멀티모듈)
- Phase 1에서 도메인 엔티티 구조가 확립됨 → 이를 기반으로 전체 규칙 확장

---

## 3. 범위 (Scope)

### In Scope
- 멀티모듈 구조 및 의존성 규칙
- Kotlin 코딩 스타일 규칙
- 네이밍 컨벤션 (클래스, 함수, 변수, 파일, DB 컬럼)
- 모듈별 패키지 구조 규칙
- 도메인별 구성 요소 배치 규칙
- JPA 엔티티 작성 규칙
- Spring 계층 규칙
- 예외 처리 규칙
- Git 커밋 메시지 규칙

### Out of Scope
- API 응답 포맷 (Phase 4에서 정의)
- 테스트 규칙 (Phase 8에서 정의)
- CI/CD 파이프라인 (Phase 9에서 정의)

---

## 4. 멀티모듈 구조

### 4.1 모듈 구성

```
near-pick/
├── app/                  # 웹 진입점
├── common/               # 공통 유틸리티
├── domain/               # 비즈니스 계약 (인터페이스 + 모델)
└── domain-nearpick/      # NearPick 구현체 (JPA + 비즈니스 로직)
```

### 4.2 모듈 역할 및 포함 요소

#### `app`
- **역할:** 애플리케이션 진입점 및 웹 계층
- **포함:** `Controller`, Spring Boot 메인 클래스, 보안 설정, Swagger 설정
- **의존성:** `implementation(domain)`, `implementation(common)`, `runtimeOnly(domain-nearpick)`

#### `common`
- **역할:** 공통 기반 기능
- **포함:** 공통 응답 클래스, 예외 클래스, JWT 유틸리티, 공통 상수
- **의존성:** 없음 (독립)

#### `domain`
- **역할:** 비즈니스 계약 정의
- **포함:** `Service` 인터페이스, `Data class` (순수 도메인 모델), `Request/Response DTO`
- **의존성:** `implementation(common)`
- **제약:** JPA 의존성 없음, 순수 Kotlin/Java만 허용

#### `domain-nearpick`
- **역할:** NearPick 도메인 구현체
- **포함:** `Service` 구현체, `JPA Repository`, `JPA Entity`, `Mapper`
- **의존성:** `implementation(domain)`, `implementation(common)`

### 4.3 의존성 흐름

```
app ──(compile)──> domain ──(compile)──> common
 └──(runtimeOnly)──> domain-nearpick ──(compile)──> domain
                                      └──(compile)──> common
```

### 4.4 `runtimeOnly` 강제의 의미

`app`이 `domain-nearpick`을 `runtimeOnly`로 선언하면:
- `app` 코드에서 `domain-nearpick` 클래스를 `import` 시 **컴파일 에러** 발생
- Controller에서 Repository 직접 주입 불가 → **Service를 통한 접근만 허용**
- Spring Boot는 런타임에 `domain-nearpick`의 Bean을 정상 탐색

---

## 5. 도메인별 구성 요소 배치

각 비즈니스 도메인(user, product, transaction 등)은 다음 요소로 구성되며,
모듈에 따라 아래와 같이 배치한다.

| 요소 | 배치 모듈 | 설명 |
|------|----------|------|
| `Controller` | `app` | REST 엔드포인트, DTO 수신/반환 |
| `Service` (인터페이스) | `domain` | 비즈니스 계약, `app`이 참조 |
| `Data class` | `domain` | 순수 비즈니스 모델, JPA 무관 |
| `Request DTO` | `domain` | Service 인터페이스 파라미터로 사용 |
| `Response DTO` | `domain` | Service 인터페이스 반환 타입 |
| `Service` 구현체 | `domain-nearpick` | 비즈니스 로직 구현, Repository 호출 |
| `Repository` | `domain-nearpick` | `JpaRepository<Entity, ID>` 확장 (이미 인터페이스) |
| `Entity` (JPA) | `domain-nearpick` | DB 매핑, `@Entity` |
| `Mapper` | `domain-nearpick` | `Entity` ↔ `Data class` 변환 |

### 도메인별 패키지 예시 (product 도메인)

```
# domain 모듈
com.nearpick.domain.product/
├── ProductService.kt              # interface
├── model/
│   └── Product.kt                 # data class (순수 도메인 모델)
└── dto/
    ├── request/
    │   └── CreateProductRequest.kt
    └── response/
        └── ProductResponse.kt

# domain-nearpick 모듈
com.nearpick.nearpick.product/
├── ProductServiceImpl.kt          # implements ProductService
├── ProductRepository.kt           # : JpaRepository<ProductEntity, Long>
├── ProductEntity.kt               # @Entity
└── ProductMapper.kt               # Entity ↔ Product (data class)

# app 모듈
com.nearpick.app.product/
└── ProductController.kt           # @RestController, uses ProductService
```

---

## 6. 네이밍 규칙

### 6.1 Kotlin 일반

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `ProductService`, `ProductEntity` |
| 인터페이스 | PascalCase (접두사 없음) | `ProductService`, `ProductRepository` |
| Enum 클래스 | PascalCase | `UserRole`, `ProductStatus` |
| Enum 값 | UPPER_SNAKE_CASE | `FLASH_SALE`, `FORCE_CLOSED` |
| 함수/메서드 | camelCase | `findNearbyProducts()` |
| 변수/프로퍼티 | camelCase | `currentLat`, `shopAddress` |
| 상수 | UPPER_SNAKE_CASE (companion object) | `MAX_RADIUS_KM` |
| 패키지 | lowercase, 단수형 | `com.nearpick.domain.product` |

### 6.2 클래스 접미사 규칙

| 종류 | 접미사 | 예시 |
|------|--------|------|
| Service 인터페이스 | `Service` | `ProductService` |
| Service 구현체 | `ServiceImpl` | `ProductServiceImpl` |
| JPA Repository | `Repository` | `ProductRepository` |
| JPA Entity | `Entity` | `ProductEntity` |
| 순수 도메인 모델 | 없음 (도메인명 그대로) | `Product`, `User` |
| Mapper | `Mapper` | `ProductMapper` |
| Controller | `Controller` | `ProductController` |
| Request DTO | `Request` | `CreateProductRequest` |
| Response DTO | `Response` | `ProductResponse` |
| 예외 | `Exception` | `ProductNotFoundException` |
| 설정 | `Config` | `SecurityConfig` |

### 6.3 파일명

| 대상 | 규칙 | 예시 |
|------|------|------|
| Kotlin 소스 파일 | PascalCase, 클래스명과 동일 | `ProductService.kt` |
| Gradle 설정 | kebab-case | `build.gradle.kts` |
| 문서 파일 | kebab-case | `phase2-convention.plan.md` |

### 6.4 DB 테이블 / 컬럼

| 대상 | 규칙 | 예시 |
|------|------|------|
| 테이블명 | snake_case, 복수형 | `users`, `flash_purchases` |
| 컬럼명 | snake_case | `business_reg_no`, `created_at` |
| 인덱스명 | `idx_{테이블}_{컬럼}` | `idx_products_location` |
| 유니크 제약 | `uq_{테이블}_{컬럼}` | `uq_wishlist_user_product` |
| FK 컬럼 | `{참조테이블_단수}_id` | `merchant_id`, `user_id` |

---

## 7. JPA Entity 규칙

1. `data class` 사용 금지 → 일반 `class` 사용 (JPA 프록시 호환)
2. `id` 필드: `Long` 타입, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
3. Enum 컬럼: 반드시 `@Enumerated(EnumType.STRING)` 사용 (숫자 저장 금지)
4. 연관관계: 기본 `FetchType.LAZY` 사용 (N+1 방지)
5. `createdAt`: `updatable = false` 설정
6. 양방향 연관관계 지양 → 단방향 우선
7. `equals()` / `hashCode()`: id 필드만 사용 (전체 필드 기반 자동 생성 금지)
8. `Entity` 클래스는 `domain-nearpick` 모듈에만 위치

---

## 8. Spring 계층 규칙

1. **Controller** (`app`): `@Valid` 유효성 검사 + Service 호출 + DTO 반환만 담당, 비즈니스 로직 금지
2. **Service 인터페이스** (`domain`): 메서드 시그니처만 정의, 트랜잭션 어노테이션 없음
3. **Service 구현체** (`domain-nearpick`): `@Transactional`, 비즈니스 규칙 구현, Repository 호출
4. **Repository** (`domain-nearpick`): `JpaRepository` 확장, 복잡한 쿼리는 `@Query` 또는 QueryDSL (Phase 4)
5. **계층 간 의존 방향**: `Controller → Service(인터페이스) → Repository` (역방향 금지)
6. **Controller에서 Repository 직접 접근 금지**: `runtimeOnly` 의존성으로 컴파일 수준 강제

---

## 9. 예외 처리 규칙

1. 커스텀 예외는 `common` 모듈의 `exception/` 패키지에 위치
2. 비즈니스 예외는 `RuntimeException` 상속
3. HTTP 상태 코드 매핑: `app` 모듈의 `@RestControllerAdvice`로 전역 처리
4. 예외 메시지: 영문 작성 (로그용), 사용자 응답 메시지는 별도 관리

---

## 10. Git 커밋 규칙

```
{type}({scope}): {subject}

types: feat | fix | docs | refactor | test | chore
scope: app | common | domain | domain-nearpick (선택)
```

| 타입 | 용도 | 예시 |
|------|------|------|
| `feat` | 새 기능 추가 | `feat(domain-nearpick): add ProductServiceImpl` |
| `fix` | 버그 수정 | `fix(domain): fix Product data class null handling` |
| `docs` | 문서 변경 | `docs: update phase2-convention plan` |
| `refactor` | 리팩토링 | `refactor(domain-nearpick): extract mapper logic` |
| `test` | 테스트 추가/수정 | `test(domain): add ProductService unit test` |
| `chore` | 빌드, 설정 변경 | `chore: add multi-module gradle config` |

---

## 11. 작업 목록 (Tasks)

- [ ] 멀티모듈 Gradle 구조 설정 (`settings.gradle.kts`, 각 `build.gradle.kts`)
- [ ] `CONVENTIONS.md` 루트 디렉토리에 생성
- [ ] Phase 1 코드를 새 모듈 구조로 이전 (`domain-nearpick`으로 Entity 이동)
- [ ] Phase 2 완료 후 `CLAUDE.md` 업데이트

---

## 12. 완료 기준 (Definition of Done)

- [ ] 4개 모듈 Gradle 빌드 정상 동작
- [ ] `app`에서 `domain-nearpick` 클래스 직접 import 시 컴파일 에러 확인
- [ ] `CONVENTIONS.md` 생성 완료
- [ ] Phase 1 Entity 코드가 `domain-nearpick` 모듈로 정상 이전

---

## 13. 다음 단계

완료 후 → `/pdca design phase2-convention`
