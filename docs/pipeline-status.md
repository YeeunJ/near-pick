# NearPick Development Pipeline Status

## Project

| 항목 | 내용 |
|------|------|
| **Name** | NearPick |
| **Level** | Enterprise |
| **Stack** | Spring Boot 4.0.3, Kotlin 2.2.21, Java 17 |
| **Started** | 2026-02-23 |
| **Last Updated** | 2026-02-27 |

## Summary

지역 기반 실시간 인기 상품 커머스 플랫폼.
소비자는 근처 인기 상품을 탐색/찜/예약/선착순 구매하고,
소상공인은 상품·할인권을 등록해 노출과 판매 기회를 확보한다.

---

## Pipeline Progress

| Phase | Name | Status | Match Rate | PR |
|-------|------|--------|------------|----|
| 1 | Schema / Terminology | ✅ Completed | 97% | - |
| 2 | Coding Convention | ✅ Completed | 95%+ | - |
| 2.5 | Docs & Workflow | ✅ Completed | - | #1 merged |
| 3 | Screen Flow Mockup | ✅ Completed | - | #2 merged |
| 4 | API Design & Implementation | ✅ Completed | 96% | #7 merged |
| 4.5 | API Quality (Swagger + Test + Flyway) | ✅ Completed | 97.5% | #8 open |
| 5 | Design System | ⏳ Pending | - | - |
| 6 | UI + API Integration | ⏳ Pending | - | - |
| 7 | SEO / Security | ⏳ Pending | - | - |
| 8 | Review | ⏳ Pending | - | - |
| 9 | Deployment | ⏳ Pending | - | - |

---

## Phase Details

### Phase 1 — Schema / Terminology ✅
- **완료일:** 2026-02-23
- **결과:** 도메인 용어 정의, 엔티티 16개 설계 (User/Profile 3종, Product, Transaction 3종 등)
- **주요 결정:** Table Per Type, Location DECIMAL(10,7), PopularityScore 별도 테이블

### Phase 2 — Coding Convention ✅
- **완료일:** 2026-02-24
- **결과:** 멀티모듈 구조 확정, 네이밍/패키지 컨벤션 정의
- **모듈 구조:** `app → domain → common`, `app →(runtimeOnly) domain-nearpick`
- **산출물:** `CONVENTIONS.md`, 모듈별 패키지 루트 정의

### Phase 2.5 — Docs & Workflow ✅
- **완료일:** 2026-02-24
- **PR:** #1 merged
- **산출물:** `README.md`, `docs/wiki/` (4개 문서), `.github/PULL_REQUEST_TEMPLATE.md`, 브랜치/PR 컨벤션

### Phase 3 — Screen Flow Mockup ✅
- **완료일:** 2026-02-25
- **PR:** #2 merged
- **산출물:** 13개 화면 목업, Phase 4 API 24개 도출
- **설계 기준 문서:** `docs/02-design/features/phase3-mockup.design.md`

### Phase 4 — API Design & Implementation ✅
- **완료일:** 2026-02-26
- **PR:** #7 (`feature/phase4-rework` → `main`) — merged
- **PDCA:** Do → Check(72%) → Act-1 → Check(96%) → Report
- **구현:** 24개 API, JWT + Spring Security, Value Objects, Mapper 패턴
- **인프라:** MySQL 8.4+ (로컬 native 또는 Docker), Spring Boot 4.x 설정
- **런타임 버그 수정 (2026-02-26):**
  - `JpaConfig` 추가: `@AutoConfigurationPackage` + `@EnableJpaRepositories` (Spring Boot 4.x `@EntityScan` 제거됨)
  - Repository 필드명 수정: `User_UserId` → `User_Id` (UserEntity.id 필드명 일치)
  - `application.properties` Jackson 설정 제거 (Spring Boot 4.x / Jackson 3.x 패키지 변경 대응)
  - `application-local.properties` 프로필 설정 이동 (profile-specific 파일 내 `spring.profiles.active` 금지)

### Phase 4.5 — API Quality (Swagger + Test + Flyway) ✅
- **완료일:** 2026-02-27
- **PR:** #8 (`feature/phase4.5-api-quality` → `main`) — open
- **PDCA:** Do → Check(97.5%) → Report
- **Swagger/OpenAPI:**
  - `springdoc-openapi-starter-webmvc-ui:3.0.0` 적용
  - `SwaggerConfig`: OpenAPI Bean + JWT Bearer SecurityScheme
  - `LocalSwaggerSecurityConfig`: `@Profile("local")` + `@Order(1)` — local에서만 무인증, prod에서 비활성화
  - 7개 Controller에 `@Tag` / `@Operation` / `@SecurityRequirement` 추가
- **테스트 (25개, 100% 통과):**
  - Controller 7개: `@SpringBootTest(webEnvironment=MOCK)` (Spring Boot 4.x `@WebMvcTest` 제거됨)
  - Service 4개: `AuthServiceImplTest`, `FlashPurchaseServiceImplTest`, `ReservationServiceImplTest`, `MerchantServiceImplTest`
  - Value Object 4개: `EmailTest`, `PasswordTest`, `LocationTest`, `BusinessRegNoTest`
  - 통합: `NearPickApplicationTests`
  - JaCoCo: Controller 60%, Service 80%, Value Object 90%, 전체 70% 목표
- **Flyway DB 마이그레이션:**
  - `spring-boot-flyway` 별도 모듈 추가 (Spring Boot 4.x auto-config 분리)
  - `V1__init_schema.sql`: 전체 스키마 DDL (8개 테이블)
  - `V2__insert_dummy_data.sql`: local 개발용 더미 데이터 (6명 사용자, 5개 상품)
  - local: `ddl-auto=validate` + `baseline-on-migrate=true`
  - test: `spring.flyway.enabled=false` (Hibernate create-drop)

---

## Deliverables

### Phase 1
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase1-schema.plan.md` | 스키마 계획서 | ✅ |
| `docs/02-design/features/phase1-schema.design.md` | 스키마 설계서 | ✅ |
| `docs/03-analysis/phase1-schema.analysis.md` | Gap Analysis | ✅ |
| `docs/04-report/phase1-schema.report.md` | 완료 보고서 | ✅ |
| `domain-nearpick/` 엔티티 9개 | UserEntity 등 | ✅ |

### Phase 2
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase2-convention.plan.md` | 컨벤션 계획서 | ✅ |
| `docs/02-design/features/phase2-convention.design.md` | 컨벤션 설계서 | ✅ |
| `docs/03-analysis/phase2-convention.analysis.md` | Gap Analysis | ✅ |
| `docs/04-report/phase2-convention.report.md` | 완료 보고서 | ✅ |
| `CONVENTIONS.md` | 코딩 컨벤션 문서 | ✅ |

### Phase 2.5
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase2-workflow.plan.md` | 워크플로우 계획서 | ✅ |
| `docs/02-design/features/phase2-workflow.design.md` | 워크플로우 설계서 | ✅ |
| `README.md` | 프로젝트 소개 | ✅ |
| `docs/wiki/00-overview.md` | 프로젝트 개요 | ✅ |
| `docs/wiki/01-domain-glossary.md` | 도메인 용어 사전 | ✅ |
| `docs/wiki/02-module-structure.md` | 모듈 구조 설명 | ✅ |
| `docs/wiki/03-dev-guide.md` | 개발 가이드 | ✅ |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 템플릿 | ✅ |

### Phase 3
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase3-mockup.plan.md` | 목업 계획서 | ✅ |
| `docs/02-design/features/phase3-mockup.design.md` | 화면 설계서 + API 목록 | ✅ |

### Phase 4
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/03-analysis/phase4-api.analysis.md` | Gap Analysis (72%→96%) | ✅ |
| `docs/04-report/phase4-api.report.md` | 완료 보고서 | ✅ |
| `app/` — Controller 7개 + Config | API 레이어 | ✅ |
| `domain/` — Service 인터페이스 + DTO | 도메인 레이어 | ✅ |
| `domain-nearpick/` — ServiceImpl + Repo + Mapper | 구현 레이어 | ✅ |
| `app/src/main/resources/application.properties` | 공통 설정 | ✅ |
| `docker-compose.yml` | MySQL 8.4 로컬 개발 | ✅ |

### Phase 4.5
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase4.5-api-quality.plan.md` | 계획서 | ✅ |
| `docs/02-design/features/phase4.5-api-quality.design.md` | 설계서 | ✅ |
| `docs/03-analysis/phase4.5-api-quality.analysis.md` | Gap Analysis (97.5%) | ✅ |
| `docs/04-report/features/phase4.5-api-quality.report.md` | 완료 보고서 | ✅ |
| `app/.../config/SwaggerConfig.kt` | OpenAPI Bean 설정 | ✅ |
| `app/.../config/LocalSwaggerSecurityConfig.kt` | local 전용 Swagger 무인증 | ✅ |
| `app/.../controller/*Controller.kt` (7개) | @Operation 어노테이션 추가 | ✅ |
| `app/src/test/.../controller/*ControllerTest.kt` (7개) | Controller 단위 테스트 | ✅ |
| `domain-nearpick/src/test/.../*ServiceImplTest.kt` (4개) | Service 단위 테스트 | ✅ |
| `domain/src/test/.../*Test.kt` (4개) | Value Object 테스트 | ✅ |
| `app/src/main/resources/db/migration/V1__init_schema.sql` | 스키마 DDL | ✅ |
| `app/src/main/resources/db/testdata/V2__insert_dummy_data.sql` | 더미 데이터 | ✅ |

---

## Workflow Notes

### PDCA Pre-Check (Phase 4부터 적용)
백엔드 코드가 있는 Phase에서 Gap Analysis 전 구동 확인 필수:
```
[Do] 구현 완료
  ↓
[Pre-Check] ./gradlew build -x test
            ./gradlew :app:bootRun  (MySQL 선행 필요)
  ↓
[Check] /pdca analyze {feature}
  ↓
[Act] /pdca iterate {feature}  (Match Rate < 90%인 경우)
```

### Spring Boot 4.x 주의사항
- `@EntityScan` 제거됨 → `@AutoConfigurationPackage` 사용
- Jackson 3.x 패키지 변경: `com.fasterxml.jackson` → `tools.jackson`
  → `spring.jackson.*` properties 일부 호환 안 됨
- Profile-specific 파일에 `spring.profiles.active` 설정 금지
- `@WebMvcTest` 완전 제거됨 → `@SpringBootTest(webEnvironment=MOCK)` + `MockMvcBuilders.webAppContextSetup` 사용
- Flyway auto-config이 `spring-boot-autoconfigure`에서 분리됨 → `spring-boot-flyway` 별도 모듈 명시 필수

### DB 환경
- 로컬: MySQL 9.2 (`/usr/local/mysql`) — 포트 3306
- `nearpick` DB + `nearpick` 유저 생성 필요
- `nearpick_test` DB 생성 필요 (테스트 전용)
- Flyway: `nearpick` DB는 `baseline-on-migrate=true`로 기존 스키마 유지, `nearpick_test`는 Flyway 비활성화
