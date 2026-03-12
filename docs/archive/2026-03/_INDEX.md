# Archive Index — 2026-03

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
