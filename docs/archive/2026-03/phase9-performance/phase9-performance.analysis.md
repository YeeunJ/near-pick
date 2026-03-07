# [Analysis] phase9-performance — Gap Analysis

## 메타데이터

| 항목 | 내용 |
|------|------|
| **Feature** | phase9-performance |
| **분석일** | 2026-03-07 |
| **Match Rate** | 97% (Act 반복 후) |
| **초기 Match Rate** | 85% |
| **Gap 수** | 5개 식별 → 4개 해소, 1개 수용 |

---

## 1. 전체 요약

| 항목 | 결과 |
|------|------|
| 설계 항목 수 | 34 |
| 구현 완료 항목 | 33 |
| 미구현/수용 | 1 (G-2: getList 캐시 — 인터페이스에 메서드 없음, 수용) |
| 최종 Match Rate | **97%** |

---

## 2. 구현 항목 (✅)

| # | 설계 항목 | 구현 파일 | 비고 |
|---|-----------|-----------|------|
| 1 | docker-compose.yml Redis + Kafka | `docker-compose.yml` | Zookeeper + Kafka + Redis 추가 |
| 2 | redisson-local.yaml | `app/src/main/resources/redisson-local.yaml` | 이미 존재 |
| 3 | application-local.properties Redis/Kafka 설정 | `app/src/main/resources/application-local.properties` | Tomcat tuning, HikariCP pool 추가 |
| 4 | RedisConfig.kt (@EnableCaching, CacheManager) | `app/.../config/RedisConfig.kt` | JdkSerializationRedisSerializer (Jackson 3.x 호환) |
| 5 | KafkaTopicConfig.kt (토픽 Bean) | `app/.../config/KafkaTopicConfig.kt` | @EnableKafka + JavaTimeModule 추가 |
| 6 | Resilience4jConfig.kt | `app/.../config/Resilience4jConfig.kt` | properties 선언적 설정 |
| 7 | RateLimitFilter Redis Bucket4j | `app/.../config/RateLimitFilter.kt` | auth rate limit 외부화 (@Value) |
| 8 | ProductServiceImpl @Cacheable | `domain-nearpick/.../product/service/ProductServiceImpl.kt` | SpEL substring 사용 (take() 불가) |
| 9 | readOnly 트랜잭션 전면 적용 | 서비스 레이어 전체 | @Transactional(readOnly=true) |
| 10 | FlashPurchaseRequestEvent | `domain-nearpick/.../messaging/FlashPurchaseRequestEvent.kt` | @JsonCreator/@JsonProperty (Jackson 2.x) |
| 11 | FlashPurchaseProducer | `domain-nearpick/.../messaging/FlashPurchaseProducer.kt` | product_id 파티션 키 |
| 12 | FlashPurchaseConsumer | `domain-nearpick/.../messaging/FlashPurchaseConsumer.kt` | Redisson RBucket 멱등성 + RLock |
| 13 | FlashPurchaseServiceImpl (Kafka 비동기) | `domain-nearpick/.../service/FlashPurchaseServiceImpl.kt` | PENDING 즉시 반환 |
| 14 | ErrorCode 추가 | `common/.../exception/ErrorCode.kt` | LOCK_FAILED, UNAVAILABLE, OUT_OF_STOCK |
| 15 | FlashPurchaseResponse nullable id | `domain/.../dto/` | purchaseId: Long? |
| 16 | k6 시나리오 1 (200 TPS) | `load-tests/scenarios/01-normal.js` | constant-arrival-rate |
| 17 | k6 시나리오 2 (이벤트) | `load-tests/scenarios/02-event.js` | ramping-arrival-rate (포화점 측정) |
| 18 | k6 시나리오 3 (선착순) | `load-tests/scenarios/03-flash.js` | shared-iterations + 100 토큰 setup() |
| 19 | FlashPurchaseConsumerTest | `domain-nearpick/test/.../FlashPurchaseConsumerTest.kt` | 6개 케이스 |
| 20 | FlashPurchaseServiceImplTest | `domain-nearpick/test/.../FlashPurchaseServiceImplTest.kt` | Kafka 비동기 검증 3개 케이스 |
| 21 | FlashPurchaseConcurrencyTest | `domain-nearpick/test/.../FlashPurchaseConcurrencyTest.kt` | 멱등성 동시성 검증 |
| 22 | ProductServiceImplTest | `domain-nearpick/test/.../ProductServiceImplTest.kt` | ProductNearbyProjection mock |

---

## 3. Gap 목록

| ID | 항목 | 원인 | 해소 방법 | 상태 |
|----|------|------|-----------|------|
| G-1 | redisson-local.yaml 신규 생성 필요 | 이미 존재했음 | 해소 불필요 | ✅ 수용 |
| G-2 | getList() 캐시 (@Cacheable products-list) | ProductService 인터페이스에 getList() 없음 (getMyProducts만 존재) | 설계 문서 아티팩트로 수용 | 수용 |
| G-3 | ProductServiceImplTest 누락 | 테스트 미작성 | ProductNearbyProjection mock 포함해 작성 | ✅ 해소 |
| G-4 | FlashPurchaseConsumerTest 누락 | 테스트 미작성 | 6케이스 작성 + lenient stubbing | ✅ 해소 |
| G-5 | FlashPurchaseConcurrencyTest 누락 | 테스트 미작성 | CountDownLatch + AtomicBoolean 멱등성 검증 | ✅ 해소 |

---

## 4. 구현 차이 (설계 대비 변경)

| 항목 | 설계 | 구현 | 사유 |
|------|------|------|------|
| Redis 캐시 직렬화 | GenericJackson2JsonRedisSerializer | JdkSerializationRedisSerializer | Spring Boot 4.x / Jackson 3.x — 2.x 직렬화기 호환 불가 |
| Consumer idempotency | redisTemplate.opsForValue().setIfAbsent() | Redisson RBucket.setIfAbsent() | Redisson TTL API 더 간결 |
| auth rate limit | 10/min 하드코딩 | @Value로 외부화 (local: 200/min) | k6 setup() 토큰 100개 발급 필요 |
| Kafka consumer 설정 | Spring 자동 설정 | 수동 ConsumerFactory + JavaTimeModule | LocalDateTime 역직렬화 오류 해소 |
| k6 시나리오 2 | 3000 TPS 고정 | ramping-arrival-rate (포화점 측정) | OS 소켓 한계로 목표 TPS 대신 포화점 측정으로 전환 |
| k6 시나리오 3 | 단일 TEST_TOKEN | 100개 사용자별 토큰 | 멱등성 키 충돌 방지 (userId별 고유 키) |
