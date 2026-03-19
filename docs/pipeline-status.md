# NearPick Development Pipeline Status

## Project

| 항목 | 내용 |
|------|------|
| **Name** | NearPick |
| **Level** | Enterprise |
| **Stack** | Spring Boot 4.0.3, Kotlin 2.2.21, Java 17 |
| **Started** | 2026-02-23 |
| **Last Updated** | 2026-03-19 (Phase 13: 완료 — Match Rate 98%, 217 tests GREEN) |

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
| 4.5 | API Quality (Swagger + Test + Flyway) | ✅ Completed | 97.5% | #8 merged |
| 5 | Design System | ✅ Completed | 95% | #9 merged |
| 6 | UI + API Integration | ➡️ near-pick-web | - | - |
| 7 | Security (백엔드) | ✅ Completed | 94% | - |
| 8 | Code Review & Quality | ✅ Completed | 98% | - |
| 9 | 고성능 아키텍처 (Redis, Kafka, 10K TPS) | ✅ Completed | 97% | - |
| 10 | 위치 & 지도 서비스 | ✅ Completed | 97% | #14 merged |
| 11 | 상품 고도화 (사진, 카테고리) | ✅ Completed | 96% | - |
| 12 | 구매 라이프사이클 정리 | ✅ Completed | 98% | - |
| 13 | 리뷰 시스템 + AI 검증 | ✅ Completed | 98% | - |
| 14 | 사용자 고도화 | ⏳ Pending | - | - |
| 15 | 종합 QA & 배포 | ⏳ Pending | - | - |
| 16 | 운영 가시성 강화 (단기) | ⏳ Pending | - | - |
| 17 | Java 21 & DB 마이그레이션 (중기) | ⏳ Pending | - | - |
| 18 | AI 재고 예측 & 멀티 리전 (장기) | ⏳ Pending | - | - |

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

### Phase 5 — Design System ✅
- **완료일:** 2026-02-28
- **PR:** #9 merged
- **구현 레포:** near-pick-web
- **PDCA:** Do(near-pick-web) → Check(95%) → Report
- **산출물:** 15개 라우트, 7개 공통 컴포넌트, 18개 타입, 46개 Mock 데이터
- **기술 선택:** Next.js 15 (App Router), TypeScript, Tailwind CSS v4, shadcn/ui v3, pnpm

### Phase 6 — UI + API Integration ➡️ near-pick-web
- **관리 레포:** [near-pick-web](https://github.com/YeeunJ/near-pick-web)
- **내용:** API 클라이언트 레이어, 인증 상태 관리 (Zustand + JWT), Mock → 실제 API 연동, 지오로케이션

---

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

### Phase 7 — Security (백엔드) ✅
- **완료일:** 2026-03-04
- **Match Rate:** 94%
- **구현:** Spring Security 설정, JWT 인증/인가, CORS, Rate Limiting (Bucket4j), 보안 헤더 (HSTS, X-Frame-Options), 입력값 검증

### Phase 8 — Code Review & Quality ✅
- **완료일:** 2026-03-05
- **Match Rate:** 98%
- **브랜치:** `feature/phase8-review`
- **구현:** P1 기능 오작동 3건, P2 성능/일관성 4건, P3 테스트 보완 2건, UI 피드백 B-1~B-5, 추가 버그 3건 (ProductType RESERVATION→GENERAL, nearby 500, 잘못된 enum 500)
- **테스트:** 34개 케이스 전체 통과

---

### Phase 9 — 고성능 아키텍처 ✅
- **완료일:** 2026-03-07
- **Match Rate:** 97% (Act 1회 반복 후)
- **브랜치:** `feature/phase9-performance`
- **구현:**
  - Redis 캐싱 (products-detail 60s, products-nearby 30s) + JdkSerializationRedisSerializer
  - Kafka 비동기 선착순 구매 (FlashPurchaseProducer → Consumer, 10 파티션)
  - Redisson 분산 락 (RLock) + 멱등성 (RBucket.setIfAbsent)
  - Redis Bucket4j Rate Limiting (LettuceBasedProxyManager, IP별)
  - Circuit Breaker (Resilience4j flashPurchase instance)
  - readOnly 트랜잭션 전면 적용
- **k6 부하 테스트 결과:**
  - 시나리오 1 (200 TPS): p95=3.71ms, 에러율 0% ✅
  - 시나리오 2 (포화점 측정): 포화점 ~174 req/s 확인, 에러율 0%
  - 시나리오 3 (선착순 100재고): 100/100 CONFIRMED, stock=0, 에러율 0% ✅
- **테스트:** 39개 케이스 전체 통과 (FlashPurchaseConsumerTest 6개, ServiceTest 3개, ConcurrencyTest 1개, ProductServiceTest 3개)
- **주요 버그 수정:** @EnableKafka 누락, LocalDateTime 역직렬화, Redis 직렬화, SpEL take(), SQL 구문 오류, Rate Limit 외부화

### Phase 10 — 위치 & 지도 서비스 ✅
- **완료일:** 2026-03-11
- **Match Rate:** 97%
- **PR:** #14 merged
- **PDCA:** Plan → Design → Do → Check(97%) → Report
- **구현:**
  - 주소 검색 API 연동 (카카오 주소 API, RestClient)
  - 소비자 위치 관리: 현 위치 + 저장 위치 최대 5개
  - 저장 위치 CRUD (별칭, 기본 위치 지정, is_default 단일성)
  - nearby 쿼리 위치 소스 선택 지원 (DIRECT/CURRENT/SAVED)
  - SavedLocationEntity + SavedLocationRepository + 3개 서비스 구현
  - KakaoLocationClient + LocationSearchServiceImpl
  - ConsumerLocationController + LocationController
  - Flyway V4__add_saved_locations.sql (saved_locations 테이블)
  - 11개 단위 테스트 (설계 요구 8개 초과 달성)
  - 8개 enhancement (Swagger, @PreAuthorize 클래스 레벨, 캐싱, apiKey blank check)

### Phase 11 — 상품 고도화 ✅
- **완료일:** 2026-03-12
- **브랜치:** `feature/phase11-product-enhancement`
- **Match Rate:** 96% (67.5/70 design items)
- **PDCA:** Plan ✅ → Design ✅ → Do ✅ → Check ✅ (96%) → Report ✅
- **Phase 11 보완 (phase11-improvement):** Cache Evict + thumbnailUrl + Strategy Pattern (Match Rate 100%)
  - PDCA: Plan ✅ → Design ✅ → Do ✅ → Check ✅ (100%) → Report ✅
- **구현 요약:**
  - 상품 이미지 업로드 (S3 Presigned URL, 최대 5장, local mock 지원)
  - 카테고리 체계 (FOOD / BEVERAGE / BEAUTY / DAILY / OTHER)
  - 음식 카테고리: 메뉴 옵션 시스템 (옵션 그룹 + 선택지, CascadeType.ALL)
  - 비음식 카테고리: 유연한 스펙 속성 (JSON TEXT, ObjectMapper 직렬화)
  - 카테고리 필터 (nearby, 목록)
- **구현 상세:**
  - 33 파일 생성/수정 (domain 8, domain-nearpick 17, app 7, common 1)
  - 9 API 엔드포인트 구현 + 4개 컨트롤러 신규
  - 5 error code 추가
  - 145+ 테스트 케이스 전체 통과
  - ImageStorageService interface 분리 (clean architecture)
  - Strategy Pattern (local/test/prod 환경별 구현)
  - 8 enhancements beyond design
- **주요 버그 수정:**
  - Redis 500 error: products-nearby cache에 JdkSerializationRedisSerializer 적용
  - Non-deterministic sort: findNearby 쿼리에 p.id ASC tiebreaker 추가
- **테스트 커버리지:**
  - domain-nearpick: 63.7% → 69.0% (+5.3%)
  - AdminServiceImpl: 0% → 100%
  - ProductImageControllerTest: 5 cases
  - ProductMenuOptionControllerTest: 4 cases
  - ProductImageServiceImplTest: 10 cases
  - ProductMenuOptionServiceImplTest: 8 cases
  - AdminServiceImplTest: 12 cases
  - ProductServiceImplTest: extended with 8 new cases
- **산출물:**
  - `docs/04-report/features/phase11-product-enhancement.report.md` (완료 보고서)

### Phase 12 — 구매 라이프사이클 정리 🔄
- **상태:** Plan ✅ → Design ⏳
- **브랜치:** `feature/phase12-purchase-lifecycle` (예정)
- **내용:**
  - Reservation 상태 플로우: `PENDING → CONFIRMED → VISITED → COMPLETED / CANCELLED / NO_SHOW`
  - FlashPurchase 상태 플로우: `PENDING → CONFIRMED → PICKED_UP / CANCELLED`
  - 방문/픽업 코드 시스템 (6자리 텍스트 코드, QR 이미지 제외)
  - NO_SHOW 자동 스케줄러 (`@Scheduled`, visitScheduledAt + 2시간 초과)
  - 취소 정책 확장 (소상공인: CONFIRMED 예약/구매 취소 가능, 재고 복원)
  - 소상공인 구매 목록 API + 상태별 필터링
  - 신규 API 8개

### Phase 13 — 리뷰 시스템 + AI 검증 🔄
- **상태:** Plan ✅ → Design ✅ → Do ✅ (2026-03-19) → Check ⏳
- **브랜치:** `feature/phase13-review-system`
- **구현:**
  - ReviewEntity / ReviewImageEntity / ReviewReplyEntity (3개 엔티티)
  - ReviewStatus enum (ACTIVE / BLINDED / DELETED)
  - 7개 ErrorCode 추가
  - Flyway V7__review_system.sql (3테이블 + products 평점 컬럼 추가)
  - ProductEntity averageRating / reviewCount 필드 추가
  - 3개 Repository (Review / ReviewImage / ReviewReply)
  - 4개 Domain 서비스 인터페이스 + Mapper
  - Claude API 비동기 AI 검증 (ReviewAiServiceImpl + NoOpReviewAiServiceImpl)
  - AsyncConfig (@EnableAsync + reviewAiExecutor 스레드풀)
  - ReviewServiceImpl / ReviewReplyServiceImpl / ReviewImageServiceImpl
  - ReviewController (9 endpoints) + AdminReviewController (3 endpoints)
  - 15개 신규 테스트 (ReviewServiceImplTest 11건, ReviewReplyServiceImplTest 4건)
  - **전체 136 tests, 0 failures GREEN**

### Phase 14 — 사용자 고도화 ⏳
- **내용 (예정):**
  - 소비자 등급 체계 (활동량 기반: 일반 / 단골 / VIP)
  - 소상공인 인증 레벨 (우수 파트너 등)
  - 단골 기능 (가게 즐겨찾기 → 새 상품 등록 시 알림)
  - 알림 시스템 기반 (FCM / APNs 연동 준비)
  - 관리자 사용자 세그먼트 조회 & 대상별 공지

### Phase 15 — 종합 QA & 배포 ⏳
- **내용 (예정):**
  - SRE: SLI/SLO 정의, 알람 설정 (Phase 9 부하 테스트 결과 기반)
  - 인프라: Kubernetes (EKS), Helm Chart, HPA
  - CI/CD: GitHub Actions → ArgoCD
  - 모니터링: Prometheus + Grafana, Loki, Jaeger
  - DB: RDS Aurora MySQL Multi-AZ + Read Replica
  - CDN: CloudFront (이미지 + API 캐시)

---

### Phase 16 — 운영 가시성 강화 ⏳ (단기 · ~1개월)
> Phase 9 Hardening 단기 개선 사항 (출처: `docs/04-report/phase9-hardening.report.md`)

- **Grafana 대시보드 템플릿 구축**
  - Prometheus 메트릭 기반 선착순 구매 현황 패널 (성공률, 중복 요청률, 재고 부족 건수)
  - 대시보드 JSON 파일 버전 관리 (`grafana/dashboards/flash-purchase.json`)
- **Alert 규칙 설정**
  - DB-Redis 재고 불일치 발생 시 관리자 슬랙/이메일 알림
  - Alertmanager 연동 또는 Grafana Alerting 사용
- **메트릭 확장**
  - 선착순 구매 처리 지연시간 (latency: `Timer` 추가)
  - 재고 부족 예측 (Redis 카운터 임계값 경보 설정)

---

### Phase 17 — Java 21 & DB 마이그레이션 ⏳ (중기 · ~3개월)
> Phase 9 Hardening 중기 개선 사항 (출처: `docs/04-report/phase9-hardening.report.md`)

- **Java 21 전환**
  - JDK 21 (LTS) toolchain 전환 (`app/build.gradle.kts`, `domain-nearpick/build.gradle.kts`)
  - `spring.threads.virtual.enabled=true` 실 효과 발현 — Tomcat Virtual Thread 스케줄링
  - 전환 전/후 TPS 정량 측정 (k6 시나리오 3 재실행)
- **PostgreSQL 마이그레이션**
  - MySQL → PostgreSQL 전환 (JSON 직렬화 효과 극대화, `jsonb` 타입 활용)
  - AdminProfile.permissions: TEXT → `jsonb` 컬럼 전환
  - Flyway: `V3__postgresql_migration.sql` 작성
- **마이크로서비스 준비**
  - Redis JSON 직렬화 기반으로 Python/Node.js 서비스 연동 가능
  - 상품 추천 서비스 별도 언어 구현 가능성 확보

---

### Phase 18 — AI 재고 예측 & 멀티 리전 ⏳ (장기 · ~6개월)
> Phase 9 Hardening 장기 개선 사항 (출처: `docs/04-report/phase9-hardening.report.md`)

- **AI 기반 선착순 재고 수요 예측**
  - Prometheus 시계열 메트릭 → 머신러닝 모델 학습
  - 상품별 수요 예측으로 적정 재고량 자동 추천 (소상공인 대시보드)
  - Claude API 활용 가능 (Phase 13 AI 리뷰 검증과 연계)
- **멀티 리전 배포**
  - Redis Cluster: 단일 노드 → 클러스터 구성 (고가용성)
  - Kafka 파티션 분산: 리전별 파티션 할당 (지연 최소화)
  - 글로벌 선착순 구매 지원 (CDN + 엣지 캐싱)
- **Zero Downtime Deployment**
  - JSON 직렬화 호환성을 활용한 Rolling 배포 전략
  - Blue/Green 배포 + Redis 캐시 워밍 자동화

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

### Phase 10
| 파일 | 설명 | 상태 |
|------|------|------|
| `docs/01-plan/features/phase10-location.plan.md` | 위치 서비스 계획서 | ✅ |
| `docs/02-design/features/phase10-location.design.md` | 위치 서비스 설계서 | ✅ |
| `docs/03-analysis/phase10-location.analysis.md` | Gap Analysis (97%) | ✅ |
| `docs/04-report/features/phase10-location.report.md` | 완료 보고서 | ✅ |
| `domain/location/` (4개 파일) | 서비스 인터페이스 + DTO | ✅ |
| `domain-nearpick/location/` (6개 파일) | Entity + Repo + ServiceImpl + Client | ✅ |
| `app/controller/ConsumerLocationController.kt` | 현재 위치 + 저장 위치 API | ✅ |
| `app/controller/LocationController.kt` | 주소 검색 API | ✅ |
| `app/src/main/resources/db/migration/V4__add_saved_locations.sql` | saved_locations 테이블 마이그레이션 | ✅ |
| `domain-nearpick/src/test/.../location/` (3개 테스트) | 11개 단위 테스트 (100% 통과) | ✅ |

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
