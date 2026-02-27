# [Plan] Phase 4.5 — API Quality (Swagger + Test)

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4.5-api-quality |
| Phase | Plan |
| 작성일 | 2026-02-26 |
| 참조 | `docs/03-analysis/phase4-api.analysis.md`, `docs/04-report/phase4-api.report.md` |

---

## 배경 및 목적

Phase 4에서 24개 API 구현을 완료했다. 이제 Phase 5(Design System) 진입 전,
API 명세를 문서화하고 Phase 4에서 작성된 기존 코드를 대상으로 커버리지 목표를 달성한다.
이번 단계에서 확립한 패턴과 기준은 Phase 5 이후 신규 개발에도 동일하게 적용된다.

### 왜 지금인가?
- Swagger UI를 통해 프론트엔드 개발자(또는 Phase 6)가 API를 직접 탐색 가능
- 테스트 코드 없이 Phase 5/6으로 진행하면 회귀 버그 감지가 어려움
- Spring Boot 4.x + Kotlin 환경에서 springdoc-openapi + JaCoCo 설정 패턴 확립 필요

### 대상 코드 범위
Phase 4에서 구현된 기존 코드를 소급 검증한다:
- Controller 7개: `AuthController`, `ProductController`, `WishlistController`, `ReservationController`, `FlashPurchaseController`, `MerchantController`, `AdminController`
- ServiceImpl: `AuthServiceImpl`, `ProductServiceImpl`, `WishlistServiceImpl`, `ReservationServiceImpl`, `FlashPurchaseServiceImpl`, `MerchantServiceImpl`, `AdminServiceImpl`
- Value Object: `Email`, `Password`, `Location`, `BusinessRegNo`

---

## 목표 (Goal)

1. **Swagger/OpenAPI 문서화**
   - springdoc-openapi-starter-webmvc-ui 적용
   - 전체 24개 엔드포인트에 `@Operation`, `@ApiResponse` 어노테이션 추가
   - Security (JWT Bearer) 전역 설정
   - `/swagger-ui.html` 접근 가능 (local 프로파일에서만 비인증 허용, prod에서는 비활성화)

2. **테스트 코드**
   - Controller 단위 테스트 (MockMvc + @SpringBootTest(webEnvironment=MOCK)): 7개 컨트롤러
   - Service 단위 테스트 (@ExtendWith(MockitoExtension)): 핵심 서비스 로직
   - 통합 테스트 (@SpringBootTest): 앱 컨텍스트 로딩 + 기본 헬스 체크

3. **DB 환경 분리 (Flyway)**
   - Flyway 도입으로 `ddl-auto=create-drop` → `validate` 전환 (local 환경)
   - `V1__init_schema.sql`: 전체 스키마 DDL 버전 관리
   - `V2__insert_dummy_data.sql`: local 개발용 더미 데이터
   - test 환경은 Flyway 비활성화 (`spring.flyway.enabled=false`)

---

## 범위 (Scope)

### In Scope
| 항목 | 설명 |
|------|------|
| springdoc-openapi 의존성 추가 | `app/build.gradle.kts` |
| OpenAPI Bean 설정 | `SwaggerConfig` in `app/` |
| Swagger 환경 분리 | `LocalSwaggerSecurityConfig` — `@Profile("local")`, `@Order(1)` |
| 컨트롤러 어노테이션 | 7개 Controller 파일 |
| Controller 테스트 | 7개 Controller × 주요 엔드포인트 |
| Service 테스트 | 3~5개 핵심 Service 메서드 |
| 통합 테스트 | `NearPickApplicationTests` 업데이트 |
| Flyway 의존성 추가 | `spring-boot-flyway`, `flyway-core`, `flyway-mysql` (Spring Boot 4.x 필수) |
| DB 마이그레이션 파일 작성 | `V1__init_schema.sql` (스키마 DDL), `V2__insert_dummy_data.sql` (더미 데이터) |
| ddl-auto 전환 + Flyway 설정 | `application-local.properties`: `validate` + Flyway 활성화 |
| test Flyway 비활성화 | `application-test.properties`: `spring.flyway.enabled=false` |

### Out of Scope
- Repository 레이어 테스트 (JPA 쿼리 자체 검증은 Phase 8 리뷰에서)
- 성능 테스트 / 부하 테스트
- E2E 테스트 (Phase 6 이후)

---

## 기술 선택

| 항목 | 선택 | 이유 |
|------|------|------|
| OpenAPI 라이브러리 | `springdoc-openapi-starter-webmvc-ui` | Spring Boot 4.x 공식 지원 |
| 테스트 프레임워크 | JUnit 5 + MockMvc + Mockito | 이미 `app/build.gradle.kts`에 설정됨 |
| 테스트 DB | MySQL (`nearpick_test` DB, create-drop) | Spring Boot 4.x에서 `@WebMvcTest` 제거로 H2 불가 → 로컬 MySQL 직접 연결 |
| 어노테이션 방식 | Kotlin DSL + `@Operation` | Kotlin 친화적 |
| DB 마이그레이션 | Flyway (`spring-boot-flyway` + `flyway-mysql`) | 스키마 버전 관리, local ddl-auto=validate 전환 |
| Swagger 환경 분리 | `LocalSwaggerSecurityConfig` (`@Profile("local")`, `@Order(1)`) | local에서만 Swagger 무인증 허용, prod에서는 비활성화 |

---

## 주요 산출물

| 파일 | 설명 |
|------|------|
| `app/src/main/kotlin/com/nearpick/app/config/SwaggerConfig.kt` | OpenAPI Bean 설정 |
| `app/src/main/kotlin/com/nearpick/app/config/LocalSwaggerSecurityConfig.kt` | local 전용 Swagger 무인증 Security Filter |
| `app/src/main/kotlin/.../controller/*Controller.kt` (7개 수정) | `@Operation` 어노테이션 추가 |
| `app/src/test/.../controller/*ControllerTest.kt` (7개 생성) | Controller 단위 테스트 |
| `app/src/test/.../NearPickApplicationTests.kt` (업데이트) | 통합 테스트 |
| `domain-nearpick/src/test/.../*ServiceImplTest.kt` (복수) | Service 단위 테스트 |
| `app/src/main/resources/db/migration/V1__init_schema.sql` | 전체 스키마 DDL (8개 테이블) |
| `app/src/main/resources/db/testdata/V2__insert_dummy_data.sql` | local 개발용 더미 데이터 |
| `app/src/main/resources/application-local.properties` (수정) | ddl-auto=validate + Flyway 설정 추가 |
| `app/src/test/resources/application-test.properties` (수정) | MySQL 연결 + spring.flyway.enabled=false |
| `app/src/main/resources/application-prod.properties.example` (수정) | Flyway migration 전용 + Swagger 비활성화 |

---

## 구현 순서

```
[Swagger + 의존성]
1. springdoc-openapi 의존성 추가 (app/build.gradle.kts)
2. SwaggerConfig 작성 + SecurityScheme(JWT Bearer) 설정
3. LocalSwaggerSecurityConfig 작성 (@Profile("local"), @Order(1)) — SecurityConfig에서 Swagger permitAll 제거

[Flyway + DB 환경 분리]
4. Flyway 의존성 추가 (spring-boot-flyway, flyway-core, flyway-mysql)
5. db/migration/V1__init_schema.sql 작성 (전체 스키마 DDL)
6. db/testdata/V2__insert_dummy_data.sql 작성 (더미 데이터, BCrypt 해시 포함)
7. application-local.properties: ddl-auto=validate + Flyway 설정 (baseline-on-migrate=true)
8. application-test.properties: spring.flyway.enabled=false 추가
9. application-prod.properties.example: Flyway migration 전용 + Swagger 비활성화

[Controller 어노테이션]
10. Controller 7개에 @Tag / @Operation / @SecurityRequirement 어노테이션 추가

[테스트]
11. Controller 테스트 작성 (MockMvc, @SpringBootTest(webEnvironment=MOCK))
12. Service 테스트 작성 (Mockito, @ExtendWith(MockitoExtension::class))
13. Value Object 테스트 작성 (Email, Password, Location, BusinessRegNo)
14. NearPickApplicationTests 통합 테스트 업데이트

[검증]
15. ./gradlew build 전체 통과 확인
```

---

## 테스트 커버리지 목표

JaCoCo를 통해 측정하며, 레이어별 목표를 분리 적용한다.

| 레이어 | 목표 | 비고 |
|--------|------|------|
| **Service** (`*ServiceImpl`) | **80%** | 핵심 비즈니스 로직 — 가장 중요 |
| **Value Object** (`Email`, `Password`, `Location`, `BusinessRegNo`) | **90%** | 단순하지만 유효성 검증이 치명적 |
| **Controller** | **60%** | 주요 경로 + 주요 오류 케이스 |
| **전체 (Overall)** | **70%** | 지속 가능한 기준선 |
| **제외** | Entity, Repository 인터페이스, Mapper | JPA 보장 범위 또는 통합 테스트에서 간접 검증 |

### 커버리지 측정 도구
- **JaCoCo** (Gradle 플러그인 `jacoco` 내장)
- 리포트: `./gradlew jacocoTestReport` → `build/reports/jacoco/test/html/index.html`
- 빌드 실패 기준: `./gradlew jacocoTestCoverageVerification` (70% 미달 시 빌드 실패)

### 테스트 작성 원칙 (Phase 5 이후 적용)
- **신규 Service 메서드 작성 시 테스트 동시 작성** (TDD-lite)
- 한 테스트는 하나의 시나리오만 검증
- given/when/then 구조 명시

---

## 성공 기준

- [ ] `./gradlew build` 성공 (테스트 포함)
- [ ] `/swagger-ui.html` 접근 시 24개 API 문서 확인 가능
- [ ] JaCoCo 커버리지: 전체 70%, Service 80%, Value Object 90% 이상
- [ ] 전체 테스트 통과율 100%
- [ ] Gap Analysis Match Rate ≥ 90%

---

## 리스크

| 리스크 | 대응 |
|--------|------|
| springdoc-openapi Spring Boot 4.x 호환성 | 최신 버전(3.0.x+) 사용, 빌드 확인 우선 |
| `domain-nearpick` runtimeOnly 제약으로 테스트 작성 어려움 | ServiceImpl 테스트는 `domain-nearpick` 모듈에 직접 작성 |
| Spring Boot 4.x에서 `@WebMvcTest` 완전 제거됨 | `@SpringBootTest(webEnvironment=MOCK)` + `MockMvcBuilders.webAppContextSetup` 사용 |
| Spring Boot 4.x Flyway auto-config 미포함 | `spring-boot-autoconfigure`에서 제거됨 — `spring-boot-flyway` 별도 모듈 명시 필수 |
| 기존 DB + Flyway baseline 충돌 | `baseline-on-migrate=true` + `baseline-version=1` 설정 — V1 skip, V2부터 적용 |
| test 환경 Flyway 충돌 | `application-test.properties`에 `spring.flyway.enabled=false` 추가 |
| prod 환경 Swagger 노출 위험 | `LocalSwaggerSecurityConfig`는 `@Profile("local")`이므로 prod에서 로드 안 됨 + properties에서 springdoc 비활성화 |
