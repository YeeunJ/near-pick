# Archive Index — 2026-03

## phase13-review-system (Phase 13: 리뷰 시스템 + Claude AI 검증)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-19 |
| **Match Rate** | 98% (설계 명세 118/118, 0 iterations) |
| **브랜치** | `feature/phase13-review-system` |
| **경로** | `docs/archive/2026-03/phase13-review-system/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase13-review-system.plan.md` | Phase 13 계획서 |
| `phase13-review-system.design.md` | Phase 13 설계서 |
| `phase13-review-system.analysis.md` | Gap Analysis (98%) |
| `phase13-review-system.report.md` | 완료 보고서 |

### 주요 완료 항목

- 리뷰 작성/조회/삭제/신고 (Reservation.COMPLETED, FlashPurchase.PICKED_UP 자격 검증)
- Claude API(claude-haiku-4-5-20251001) 비동기 AI 검증 (pass/fail/need_review, blindPending fallback)
- 소상공인 답글 작성/삭제 (상품 소유자만 가능)
- 리뷰 이미지 Presigned URL 발급 + 업로드 확인 (최대 3장)
- 관리자 블라인드/해제 + 관리자 큐 조회 (blindPending=true OR reportCount>=3)
- products 평점 집계 자동 갱신 (averageRating, reviewCount)
- Flyway V7 (리뷰 스키마) + V8 (rating TINYINT→INT)
- @Async reviewAiExecutor 스레드 풀 (corePoolSize=2, maxPoolSize=5)
- 12개 API 엔드포인트 (ReviewController 9, AdminReviewController 3)
- 테스트 217개 전체 통과 (+81: ReviewImageServiceImplTest 신규 6, ReviewServiceImplTest +4, ReviewReplyServiceImplTest +2)

---

## phase12-purchase-lifecycle (Phase 12: 구매 라이프사이클 완성)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-13 |
| **Match Rate** | 98% (Core Logic 100%, Tests 91%) |
| **브랜치** | `feature/phase11-product-enhancement` |
| **경로** | `docs/archive/2026-03/phase12-purchase-lifecycle/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase12-purchase-lifecycle.plan.md` | Phase 12 계획서 |
| `phase12-purchase-lifecycle.design.md` | Phase 12 설계서 |
| `phase12-purchase-lifecycle.analysis.md` | Gap Analysis (92% → 98%) |
| `phase12-purchase-lifecycle.report.md` | 완료 보고서 |

### 주요 완료 항목

- 상품 상태 고도화: PAUSED API (pause/resume), FORCE_CLOSED 버그 수정, stock=0 자동 PAUSED, availableFrom/Until 시행
- Reservation 플로우 완성: visitCode 생성, COMPLETED/NO_SHOW 상태, 소상공인 취소+재고복원
- FlashPurchase 플로우 완성: pickupCode 생성, PICKED_UP 상태, 소상공인 취소+DB/Redis 재고복원
- 재고 복원 정책: 예약·선착순 취소 시 재고 복원, Redis+DB 이중 복원
- 스케줄러 2개: ReservationScheduler (processNoShow, processExpiredPending), ProductScheduler (pauseExpiredProducts, syncRedisStockWithDb)
- Reservation 생성 시 재고 감소 (오버부킹 방지)
- 11개 신규 API 엔드포인트, 9개 ErrorCode 추가
- 테스트 190개 전체 통과 (Phase 12에서 15개 신규 추가)

---

## phase11-product-enhancement (Phase 11: 상품 고도화 — 카테고리/이미지/메뉴옵션/스펙)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-13 |
| **Match Rate** | 96% (1회 반복 없음) |
| **브랜치** | `feature/phase11-product-enhancement` |
| **PR** | #15 merged |
| **경로** | `docs/archive/2026-03/phase11-product-enhancement/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase11-product-enhancement.plan.md` | Phase 11 계획서 |
| `phase11-product-enhancement.design.md` | Phase 11 설계서 |
| `phase11-product-enhancement.analysis.md` | Gap Analysis (96%) |
| `phase11-product-enhancement.report.md` | 완료 보고서 |

### 주요 완료 항목

- 상품 카테고리 체계 (FOOD/BEVERAGE/BEAUTY/DAILY/OTHER)
- 상품 이미지 업로드 (S3 Presigned URL, 최대 5장)
- 메뉴 옵션 시스템 (음식/음료 카테고리, 옵션 그룹 + 선택지)
- 비음식 카테고리 스펙 속성 (JSON TEXT)
- 카테고리 필터 (nearby, 목록)
- 9개 API 엔드포인트 + 4개 컨트롤러 신규

---

## phase11-improvement (Phase 11 보완: Cache Evict + thumbnailUrl + Strategy Pattern)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-13 |
| **Match Rate** | 100% (29/29, 0 iterations) |
| **브랜치** | `feature/phase11-product-enhancement` |
| **경로** | `docs/archive/2026-03/phase11-improvement/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase11-improvement.plan.md` | Phase 11 보완 계획서 |
| `phase11-improvement.design.md` | Phase 11 보완 설계서 |
| `phase11-improvement.analysis.md` | Gap Analysis (100%, 29/29) |
| `phase11-improvement.report.md` | 완료 보고서 |

### 주요 완료 항목

- Cache Invalidation: `ProductImageServiceImpl` (saveImageUrl/deleteImage/reorderImages), `ProductMenuOptionServiceImpl` (saveMenuOptions/deleteMenuOptionGroup) — `@CacheEvict(products-detail)`
- thumbnailUrl 수정: `findNearby` 네이티브 쿼리에 `product_images` LEFT JOIN (display_order=0), `ProductNearbyProjection.thumbnailUrl: String?` 추가
- Strategy Pattern — LocationClient: 인터페이스 분리 (`domain/location/`), `KakaoLocationClient @Profile("!test")`, `NoOpLocationClient @Profile("test")`
- Strategy Pattern — FlashPurchaseEventProducer: 인터페이스 분리, `KafkaFlashPurchaseProducer @Profile("!test")`, `NoOpFlashPurchaseEventProducer @Profile("test")`, Consumer/DlqConsumer `@Profile("!test")` 격리
- Strategy Pattern — ImageStorageService: `LocalImageStorageService @Profile("local | test")`로 확장, `NoOpImageStorageService` 삭제
- 테스트 5건 추가: LocationSearchServiceImplTest, FlashPurchaseServiceImplTest 목 타입 변경, ProductServiceImplTest thumbnailUrl, ProductImageServiceImplTest/@CacheEvict 3건, ProductMenuOptionServiceImplTest/@CacheEvict 2건

---


## phase10-location (Phase 10: 위치 & 지도 서비스)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-11 |
| **Match Rate** | 97% (0 iterations) |
| **브랜치** | `feature/phase10-location` |
| **PR** | #14 |
| **경로** | `docs/archive/2026-03/phase10-location/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase10-location.plan.md` | Phase 10 계획서 |
| `phase10-location.design.md` | Phase 10 설계서 |
| `phase10-location.analysis.md` | Gap Analysis (97%) |
| `phase10-location.report.md` | 완료 보고서 |

### 주요 완료 항목

- 현재 위치 갱신: `PATCH /api/consumers/me/location` (ConsumerProfile.currentLat/Lng)
- 저장 위치 CRUD: `GET/POST/PUT/DELETE/PATCH /api/consumers/me/locations` (최대 5개, default 단일성)
- 카카오 주소 검색: `GET /api/location/search?query=...` (KakaoLocationClient, RestClient)
- nearby locationSource: `DIRECT/CURRENT/SAVED` 파라미터 추가
- Flyway V4: `saved_locations` 테이블 마이그레이션
- BucketProvider 추출: RateLimitFilter 테스트 가능성 개선
- 단위 테스트 11개 추가 (총 app+domain-nearpick 81개 / 0 failures)

---

## phase9-hardening (Phase 9 성능 강화 — 5가지 아키텍처 개선)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-11 |
| **Match Rate** | 100% (23/23, 0 iterations) |
| **브랜치** | `feature/phase9-hardening` |
| **PR** | #13 |
| **경로** | `docs/archive/2026-03/phase9-hardening/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase9-hardening.design.md` | 5가지 개선 설계서 |
| `phase9-hardening.analysis.md` | Gap Analysis (100%, 0 iterations) |
| `phase9-hardening.report.md` | 완료 보고서 |

### 주요 완료 항목

- Virtual Threads: `spring.threads.virtual.enabled=true` (Java 21 대비)
- Redis 원자적 재고 카운터: `RAtomicLong.addAndGet()` — DB 비관적 락 + Redisson RLock 제거
- Micrometer + Prometheus + Grafana: `flash.purchase` 커스텀 메트릭, docker-compose 통합
- Redis JSON 직렬화: `JdkSerializationRedisSerializer` → `GenericJackson2JsonRedisSerializer` (Jackson 2.x)
- Idempotency 시간 단위 전환 (일→시간) + `FlashPurchaseDlqConsumer` (FAILED 상태 기록)
- 단위 테스트 40개 전체 통과 (신규 7개 포함)

---

## phase9-performance (Phase 9: 고성능 아키텍처)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-07 |
| **Match Rate** | 97% |
| **브랜치** | `feature/phase9-performance` |
| **경로** | `docs/archive/2026-03/phase9-performance/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase9-performance.plan.md` | Phase 9 계획서 |
| `phase9-performance.design.md` | Phase 9 설계서 |
| `phase9-performance.analysis.md` | Gap Analysis (85% → 97%, Act 1회) |
| `phase9-performance.report.md` | 완료 보고서 |

### 주요 완료 항목

- Redis 캐싱 (products-detail 60s, products-nearby 30s), JdkSerializationRedisSerializer
- Kafka 비동기 선착순 구매 (10 파티션, @EnableKafka, JavaTimeModule)
- Redisson 분산 락 (RLock) + 멱등성 (RBucket.setIfAbsent)
- Redis Bucket4j Rate Limiting (LettuceBasedProxyManager, auth 외부화)
- Circuit Breaker (Resilience4j flashPurchase instance)
- k6 시나리오 1 (p95=3.71ms ✅), 시나리오 3 (100/100 CONFIRMED, stock=0 ✅)
- 단위 테스트 39개 통과 (ConsumerTest 6, ServiceTest 3, ConcurrencyTest 1, ProductServiceTest 3)
- 주요 버그 수정: @EnableKafka 누락, JavaTimeModule, JdkSerializer, SpEL, SQL, Rate Limit

---

## controller (Phase 8: Code Review & Quality)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-06 |
| **Match Rate** | 98% |
| **브랜치** | `feature/phase8-review` |
| **경로** | `docs/archive/2026-03/controller/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase8-review.plan.md` | Phase 8 계획서 |
| `phase8-review.design.md` | Phase 8 설계서 (9개 이슈 + B-1~B-5) |
| `phase8-review.analysis.md` | Phase 8 Gap Analysis |
| `phase8-review.report.md` | Phase 8 완료 보고서 |
| `controller.analysis.md` | PDCA Check — Gap Analysis (98%) |
| `controller.report.md` | PDCA 완료 보고서 (전체 로드맵 포함) |

### 주요 완료 항목

- P1: AdminController 200 응답, WishlistService RESOURCE_NOT_FOUND, GlobalExceptionHandler 핸들러 추가
- P2: 인덱스 추가, WishlistAddResponse DTO, 최대 200개 제한, @RequestBody @Valid 통일
- P3: WishlistServiceImplTest (6케이스), RateLimitFilterTest (5케이스)
- UI 피드백: B-1~B-5 DTO 필드 5건
- 추가 버그픽스: ProductType RESERVATION 복원, nearby BigDecimal 수정, HttpMessageNotReadableException 400 처리
