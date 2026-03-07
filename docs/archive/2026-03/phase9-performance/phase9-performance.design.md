# [Design] phase9-performance — 고성능 아키텍처 설계

## 메타데이터

| 항목 | 내용 |
|------|------|
| **Feature** | phase9-performance |
| **Plan 참조** | `docs/01-plan/features/phase9-performance.plan.md` |
| **작성일** | 2026-03-06 |
| **목표 TPS** | 평시 200 / 이벤트 3,000 / 선착순 10,000 |

---

## 1. 아키텍처 개요

```
Client
  │
  ▼
[RateLimitFilter] ← Redis Bucket4j (분산)
  │
  ▼
[Controller] app 모듈
  │
  ├─ 상품 조회 → [ProductServiceImpl] ─┬─ @Cacheable → Redis Cache
  │                                    └─ Cache Miss → MySQL (readOnly)
  │
  ├─ 선착순 구매 → [FlashPurchaseServiceImpl]
  │                    │
  │                    ├─ Redisson Lock (분산 락)
  │                    ├─ Idempotency 체크 → Redis
  │                    └─ KafkaProducer → [flash-purchase-requests]
  │                                            │
  │                              [FlashPurchaseConsumer]
  │                                            ├─ 재고 감소 처리
  │                                            └─ 실패 → [flash-purchase-dlq]
  │
  └─ 일반 서비스 → Circuit Breaker → DB
```

---

## 2. 의존성 추가

### `app/build.gradle.kts`

```kotlin
// Redis
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.springframework.boot:spring-boot-starter-cache")

// Kafka
implementation("org.springframework.kafka:spring-kafka")

// Redisson (Distributed Lock)
implementation("org.redisson:redisson-spring-boot-starter:3.36.0")

// Bucket4j Redis (분산 Rate Limiting)
implementation("com.bucket4j:bucket4j-redis:8.10.1")

// Resilience4j Circuit Breaker
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
implementation("org.springframework.boot:spring-boot-starter-aop")  // Resilience4j 필수

// 기존 bucket4j-core는 그대로 유지
```

### `domain-nearpick/build.gradle.kts`

```kotlin
// Spring Cache (Cacheable 어노테이션 런타임)
implementation("org.springframework.boot:spring-boot-starter-cache")

// Kafka (KafkaTemplate 주입)
implementation("org.springframework.kafka:spring-kafka")

// Redisson (RedissonClient 주입)
implementation("org.redisson:redisson-spring-boot-starter:3.36.0")
```

---

## 3. 인프라 설정

### `docker-compose.yml` 수정

```yaml
services:
  mysql: # 기존 유지

  redis:
    image: redis:7-alpine
    container_name: nearpick-redis
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.0
    container_name: nearpick-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.7.0
    container_name: nearpick-kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "9092:9092"
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 15s
      timeout: 10s
      retries: 5

volumes:
  nearpick-mysql-data:
```

### `application-local.properties` 추가

```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=nearpick-cg
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.nearpick.*

# Redisson (단일 노드)
spring.data.redisson.config=classpath:redisson-local.yaml
```

### `app/src/main/resources/redisson-local.yaml` (신규)

```yaml
singleServerConfig:
  address: "redis://localhost:6379"
  connectionPoolSize: 10
  connectionMinimumIdleSize: 5
```

---

## 4. 신규 설정 클래스 (app 모듈)

### `app/config/RedisConfig.kt`

```kotlin
@Configuration
@EnableCaching
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues()

        val cacheConfigs = mapOf(
            "products-list"   to defaultConfig.entryTtl(Duration.ofSeconds(30)),
            "products-detail" to defaultConfig.entryTtl(Duration.ofSeconds(60)),
            "products-nearby" to defaultConfig.entryTtl(Duration.ofSeconds(30)),
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }
}
```

### `app/config/KafkaTopicConfig.kt`

```kotlin
@Configuration
class KafkaTopicConfig {

    @Bean
    fun flashPurchaseRequestTopic(): NewTopic =
        TopicBuilder.name("flash-purchase-requests")
            .partitions(10)       // productId 기준 파티셔닝
            .replicas(1)          // 로컬: 1, 프로덕션: 3
            .build()

    @Bean
    fun flashPurchaseDlqTopic(): NewTopic =
        TopicBuilder.name("flash-purchase-dlq")
            .partitions(1)
            .replicas(1)
            .build()
}
```

### `app/config/Resilience4jConfig.kt`

```kotlin
@Configuration
class Resilience4jConfig {
    // application.yml에서 선언적 설정 사용 (아래 참조)
    // Circuit Breaker 적용 대상: DB 쓰기 작업, Kafka Producer
}
```

`application.properties` 추가:
```properties
resilience4j.circuitbreaker.instances.flashPurchase.sliding-window-size=10
resilience4j.circuitbreaker.instances.flashPurchase.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.flashPurchase.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.flashPurchase.permitted-number-of-calls-in-half-open-state=3
```

---

## 5. RateLimitFilter 수정 (Redis Bucket4j)

### `app/config/RateLimitFilter.kt` 변경

```kotlin
@Component
@Order(1)
class RateLimitFilter(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
) : OncePerRequestFilter() {

    // LettuceBasedProxyManager → Redis 분산 Bucket
    private val proxyManager: ProxyManager<String> by lazy {
        LettuceBasedProxyManager.builderFor(redisTemplate.connectionFactory!!)
            .build()
    }

    private fun getBandwidth(path: String): Bandwidth = when {
        path.contains("/login")  -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))
        path.contains("/signup") -> Bandwidth.classic(5,  Refill.intervally(5,  Duration.ofMinutes(1)))
        else                     -> Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)))
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val ip = request.getHeader("X-Forwarded-For") ?: request.remoteAddr
        val path = request.requestURI
        val key = "rate:$ip:${if (path.contains("/login")) "login" else if (path.contains("/signup")) "signup" else "api"}"

        val config = BucketConfiguration.builder()
            .addLimit(getBandwidth(path))
            .build()

        val bucket = proxyManager.builder().build(key, config)

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            response.status = 429
            response.writer.write("""{"success":false,"message":"Too Many Requests"}""")
        }
    }
}
```

---

## 6. 상품 캐싱 (domain-nearpick)

### `ProductServiceImpl.kt` 수정

```kotlin
// 추가 import: org.springframework.cache.annotation.*

@Service
@Transactional
class ProductServiceImpl(...) : ProductService {

    @Cacheable(value = ["products-list"], key = "#page + ':' + #size + ':' + #sort")
    @Transactional(readOnly = true)
    override fun getList(page: Int, size: Int, sort: SortType): ProductListResponse { ... }

    @Cacheable(value = ["products-detail"], key = "#id")
    @Transactional(readOnly = true)
    override fun getDetail(id: Long): ProductDetailResponse { ... }

    @Cacheable(value = ["products-nearby"], key = "#lat.toString().take(6) + ':' + #lng.toString().take(6) + ':' + #radius + ':' + #sort")
    @Transactional(readOnly = true)
    override fun getNearby(lat: Double, lng: Double, radius: Int, sort: SortType): List<ProductSummaryResponse> { ... }

    @CacheEvict(value = ["products-list", "products-nearby"], allEntries = true)
    override fun create(merchantId: Long, request: CreateProductRequest): ProductDetailResponse { ... }

    @CacheEvict(value = ["products-list", "products-detail", "products-nearby"], allEntries = true)
    override fun update(merchantId: Long, productId: Long, request: UpdateProductRequest): ProductDetailResponse { ... }
}
```

---

## 7. readOnly 트랜잭션 정리

적용 대상:

| 클래스 | 메서드 |
|--------|--------|
| `ProductServiceImpl` | `getList`, `getDetail`, `getNearby` |
| `WishlistServiceImpl` | `getMyWishlists` |
| `ReservationServiceImpl` | `getMyReservations`, `getDetail` |
| `MerchantServiceImpl` | `getDashboard` |
| `AdminServiceImpl` | `getUsers`, `getProducts` |

---

## 8. 선착순 구매 비동기화 (domain-nearpick)

### 신규 파일 구조

```
domain-nearpick/src/main/kotlin/com/nearpick/nearpick/
  transaction/
    messaging/
      FlashPurchaseRequestEvent.kt   ← Kafka 메시지 DTO
      FlashPurchaseProducer.kt       ← KafkaTemplate 발행
      FlashPurchaseConsumer.kt       ← @KafkaListener 처리
    service/
      FlashPurchaseServiceImpl.kt    ← (수정) Producer 호출로 변경
```

### `FlashPurchaseRequestEvent.kt`

```kotlin
data class FlashPurchaseRequestEvent(
    val idempotencyKey: String,   // "{userId}-{productId}-{yyyyMMdd}"
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val requestedAt: LocalDateTime = LocalDateTime.now()
)
```

### `FlashPurchaseProducer.kt`

```kotlin
@Component
class FlashPurchaseProducer(
    private val kafkaTemplate: KafkaTemplate<String, FlashPurchaseRequestEvent>
) {
    fun send(event: FlashPurchaseRequestEvent) {
        // key = productId → 같은 파티션 보장 → 순서 처리
        kafkaTemplate.send("flash-purchase-requests", event.productId.toString(), event)
    }
}
```

### `FlashPurchaseConsumer.kt`

```kotlin
@Component
class FlashPurchaseConsumer(
    private val flashPurchaseRepository: FlashPurchaseRepository,
    private val productRepository: ProductRepository,
    private val redissonClient: RedissonClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    @KafkaListener(
        topics = ["flash-purchase-requests"],
        groupId = "flash-purchase-cg",
        containerFactory = "flashPurchaseContainerFactory"
    )
    @Transactional
    fun consume(event: FlashPurchaseRequestEvent) {
        // 1. Idempotency 체크 (Redis SETNX)
        val idempotencyKey = "idempotency:flash:${event.idempotencyKey}"
        val isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofDays(1))
        if (isNew != true) return  // 중복 요청 무시

        // 2. Distributed Lock 획득
        val lock = redissonClient.getLock("lock:flash:product:${event.productId}")
        val acquired = lock.tryLock(3, 10, TimeUnit.SECONDS)
        if (!acquired) throw BusinessException(ErrorCode.FLASH_PURCHASE_LOCK_FAILED)

        try {
            // 3. 재고 확인 및 감소
            val product = productRepository.findByIdWithLock(event.productId)
                ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            if (product.stock < event.quantity) throw BusinessException(ErrorCode.OUT_OF_STOCK)
            product.stock -= event.quantity

            // 4. FlashPurchase 엔티티 저장
            flashPurchaseRepository.save(
                FlashPurchaseEntity(
                    userId = event.userId,
                    productId = event.productId,
                    quantity = event.quantity,
                    status = FlashPurchaseStatus.CONFIRMED
                )
            )
        } finally {
            lock.unlock()
        }
    }
}
```

### `FlashPurchaseServiceImpl.kt` 수정

```kotlin
@Service
class FlashPurchaseServiceImpl(
    private val producer: FlashPurchaseProducer,
    private val flashPurchaseRepository: FlashPurchaseRepository,
    // findByIdWithLock 유지 (Consumer에서 사용)
) : FlashPurchaseService {

    @CircuitBreaker(name = "flashPurchase", fallbackMethod = "purchaseFallback")
    override fun purchase(userId: Long, request: FlashPurchaseRequest): FlashPurchaseResponse {
        val idempotencyKey = "$userId-${request.productId}-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}"

        producer.send(FlashPurchaseRequestEvent(
            idempotencyKey = idempotencyKey,
            userId = userId,
            productId = request.productId,
            quantity = request.quantity
        ))

        // 즉시 PENDING 상태 반환 (비동기 처리)
        return FlashPurchaseResponse(
            id = null,
            productId = request.productId,
            quantity = request.quantity,
            status = FlashPurchaseStatus.PENDING,
            purchasedAt = LocalDateTime.now()
        )
    }

    fun purchaseFallback(userId: Long, request: FlashPurchaseRequest, ex: Exception): FlashPurchaseResponse {
        throw BusinessException(ErrorCode.FLASH_PURCHASE_UNAVAILABLE)
    }
}
```

---

## 9. ErrorCode 추가

`common` 모듈 `ErrorCode.kt`에 추가:

```kotlin
FLASH_PURCHASE_LOCK_FAILED(HttpStatus.CONFLICT, "선착순 처리 중 충돌이 발생했습니다. 다시 시도해주세요."),
FLASH_PURCHASE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "선착순 구매 서비스가 일시적으로 이용 불가합니다."),
OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
```

---

## 10. DTO 변경

### `FlashPurchaseResponse` (domain 모듈)

```kotlin
data class FlashPurchaseResponse(
    val id: Long?,                          // 비동기 처리 시 null 가능
    val productId: Long,
    val quantity: Int,
    val status: FlashPurchaseStatus,
    val purchasedAt: LocalDateTime
)
```

---

## 11. 테스트 설계

| 테스트 클래스 | 위치 | 케이스 |
|-------------|------|--------|
| `ProductCacheTest` | `domain-nearpick/test` | 캐시 히트/미스, TTL, Evict |
| `FlashPurchaseConsumerTest` | `domain-nearpick/test` | 정상처리, 중복요청 무시, 재고부족 DLQ |
| `RateLimitFilterTest` | `app/test` | Redis 기반 Rate Limit (기존 테스트 수정) |
| `FlashPurchaseIntegrationTest` | `app/test` | 동시 100 요청 → 재고 초과 없음 |

---

## 12. 부하 테스트 설계 (k6)

### 파일 구조

```
load-tests/
  common/
    auth.js          ← 토큰 발급 헬퍼
    config.js        ← BASE_URL, 공통 thresholds
  scenarios/
    01-normal.js     ← 평시 200 TPS 시나리오
    02-event.js      ← 이벤트 3,000 TPS 시나리오
    03-flash.js      ← 선착순 10,000 TPS 시나리오
  run-all.sh         ← 전체 실행 스크립트
```

### `load-tests/common/config.js`

```javascript
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const defaultThresholds = {
  http_req_failed:   ['rate<0.01'],   // 에러율 1% 미만
  http_req_duration: ['p(95)<500'],   // p95 응답 500ms 미만
};
```

### 시나리오 1 — 평시 (200 TPS)

**`load-tests/scenarios/01-normal.js`**

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, defaultThresholds } from '../common/config.js';

export const options = {
  scenarios: {
    normal_load: {
      executor: 'constant-arrival-rate',
      rate: 200,          // 200 req/s
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    ...defaultThresholds,
    http_req_duration: ['p(95)<200'],  // 평시는 200ms 이하 목표
  },
};

export default function () {
  // 상품 목록 조회 (캐시 히트 확인)
  const listRes = http.get(`${BASE_URL}/api/products?page=0&size=10&sort=POPULARITY`);
  check(listRes, { 'list status 200': (r) => r.status === 200 });

  // 근처 상품 조회
  const nearbyRes = http.get(`${BASE_URL}/api/products/nearby?lat=37.5563&lng=126.9236&radius=2&sort=POPULARITY`);
  check(nearbyRes, { 'nearby status 200': (r) => r.status === 200 });

  sleep(0.5);
}
```

### 시나리오 2 — 이벤트 (3,000 TPS)

**`load-tests/scenarios/02-event.js`**

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, defaultThresholds } from '../common/config.js';

export const options = {
  scenarios: {
    event_load: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 500,
      maxVUs: 3000,
      stages: [
        { target: 3000, duration: '30s' },  // 30초 동안 3,000 TPS로 램프업
        { target: 3000, duration: '30s' },  // 30초 유지
      ],
    },
  },
  thresholds: {
    ...defaultThresholds,
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.02'],  // 이벤트 시 에러율 2% 허용
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/products?page=0&size=10&sort=POPULARITY`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
```

### 시나리오 3 — 선착순 (10,000 동시 요청)

**`load-tests/scenarios/03-flash.js`**

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../common/config.js';

// 사전 조건: 상품 ID 1번, 재고 100개 설정

const PRODUCT_ID = 1;
const STOCK = 100;  // 재고 수량 (검증 기준)

export const options = {
  scenarios: {
    flash_purchase: {
      executor: 'shared-iterations',
      vus: 10000,
      iterations: 10000,  // 10,000명이 동시에 1번씩 구매 시도
      maxDuration: '30s',
    },
  },
  thresholds: {
    // 핵심: 재고(100개) 초과 구매는 허용하지 않음 → 별도 DB 검증
    http_req_failed:   ['rate<0.01'],  // 5xx 에러율 1% 미만 (429는 정상)
    http_req_duration: ['p(95)<2000'], // 선착순 특성상 2s 허용
  },
};

export default function () {
  const payload = JSON.stringify({ productId: PRODUCT_ID, quantity: 1 });
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${__ENV.TEST_TOKEN}`,  // 사전 발급 토큰
  };

  const res = http.post(`${BASE_URL}/api/flash-purchases`, payload, { headers });

  // 200(성공) 또는 409(재고부족) 또는 429(rate limit) 만 허용
  check(res, {
    'acceptable status': (r) => [200, 409, 429].includes(r.status),
  });
}
```

### `load-tests/run-all.sh`

```bash
#!/bin/bash
set -e

BASE_URL=${BASE_URL:-http://localhost:8080}
TEST_TOKEN=${TEST_TOKEN:-$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"consumer1@test.com","password":"test1234"}' | jq -r '.data.accessToken')}

echo "=== 시나리오 1: 평시 200 TPS ==="
k6 run -e BASE_URL=$BASE_URL scenarios/01-normal.js

echo "=== 시나리오 2: 이벤트 3,000 TPS ==="
k6 run -e BASE_URL=$BASE_URL scenarios/02-event.js

echo "=== 시나리오 3: 선착순 10,000 동시 요청 ==="
k6 run -e BASE_URL=$BASE_URL -e TEST_TOKEN=$TEST_TOKEN scenarios/03-flash.js

echo "=== 완료 ==="
```

### 검증 항목

| 시나리오 | 확인 항목 | 합격 기준 |
|----------|-----------|-----------|
| 평시 | p95 응답시간 | < 200ms |
| 평시 | Redis 캐시 히트율 | > 80% (Redis monitor로 확인) |
| 이벤트 | p95 응답시간 | < 500ms |
| 이벤트 | 에러율 | < 2% |
| 선착순 | DB 실제 구매 건수 | ≤ 100건 (재고 초과 없음) |
| 선착순 | 5xx 에러율 | < 1% (429는 정상) |

### 선착순 시나리오 사후 DB 검증

```sql
-- 테스트 후 실행: 구매 건수 확인
SELECT COUNT(*) FROM flash_purchases WHERE product_id = 1 AND status = 'CONFIRMED';
-- 결과가 100 이하여야 합격

-- 상품 재고 확인
SELECT stock FROM products WHERE id = 1;
-- 결과가 0 이상이어야 합격 (음수면 초과 구매 발생)
```

---

## 13. 구현 순서 (Do Phase)

```
Step 1  docker-compose.yml — Redis + Zookeeper + Kafka 추가
Step 2  application-local.properties — Redis/Kafka 설정 추가
Step 3  redisson-local.yaml — Redisson 단일 노드 설정
Step 4  build.gradle.kts (app, domain-nearpick) — 의존성 추가
Step 5  RedisConfig.kt — CacheManager, RedisTemplate 설정
Step 6  KafkaTopicConfig.kt — 토픽 Bean 정의
Step 7  ProductServiceImpl — @Cacheable + readOnly 트랜잭션
Step 8  나머지 Service — readOnly 트랜잭션 정리
Step 9  RateLimitFilter — Redis Bucket4j 전환
Step 10 FlashPurchaseRequestEvent, Producer, Consumer 구현
Step 11 FlashPurchaseServiceImpl — Kafka 비동기 전환
Step 12 ErrorCode 추가
Step 13 FlashPurchaseResponse nullable id 처리
Step 14 Resilience4jConfig + application.properties
Step 15 단위 테스트 (ProductCacheTest, ConsumerTest, IntegrationTest)
Step 16 로컬 구동 검증 (docker-compose up → bootRun)
Step 17 load-tests/ 스크립트 작성
Step 18 k6 부하 테스트 실행 및 결과 분석
Step 19 병목 확인 시 튜닝 후 재실행
```

---

## 14. 주의사항

| 항목 | 내용 |
|------|------|
| Resilience4j 호환 | `resilience4j-spring-boot3` + Spring Boot 4.x 호환성 구동 시 확인 필요 |
| Kafka 비동기 응답 | 기존 `FlashPurchaseResponse.id` → nullable (`Long?`) 로 변경 필요 |
| 기존 RateLimitFilterTest | Redis 없는 환경 → MockBean 또는 TestContainer 사용 |
| redisson-spring-boot-starter | Spring Boot 4.x는 3.36.0+ 사용 |
| `findByIdWithLock` | Consumer에서도 재사용 (기존 메서드 유지) |
| k6 선착순 테스트 | `TEST_TOKEN` 환경변수 사전 발급 필요 (run-all.sh 자동화) |
| Rate Limit 충돌 | 부하 테스트 실행 시 로컬 Rate Limit을 높게 설정하거나 테스트 전용 헤더 우회 |
