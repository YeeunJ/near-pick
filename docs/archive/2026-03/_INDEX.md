# Archive Index — 2026-03

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
