# Design: phase9-hardening

> Phase 9 아키텍처 분석 결과 도출된 5가지 성능·운영 개선 구현 설계

## 목표

10년 이상 경력 팀의 기준에서 식별된 주요 결함 5가지 해소:
- DB 비관적 락 병목 (10,000 TPS 불가)
- Redis JDK 직렬화 (크로스 언어 불가, 스키마 변경 취약)
- 운영 가시성 부재 (Prometheus/Grafana 없음)
- Virtual Threads 미활성화 (Java 21 대비)
- 중복 구매 허용 (일 단위 idempotency) + DLQ 이력 없음

---

## 개선 항목별 설계

### 개선 1. Virtual Threads

**목표**: Java 21 Virtual Threads 준비 (현재 Java 17, 무시됨)

- `application.properties`에 `spring.threads.virtual.enabled=true` 추가
- `application-local.properties`에서 Tomcat 스레드풀 설정 제거 (Virtual Threads로 대체)
  - 제거 대상: `server.tomcat.threads.max`, `server.tomcat.threads.min-spare`, `server.tomcat.accept-count`

---

### 개선 2. Redis 원자적 재고 카운터

**목표**: DB 비관적 락 + Redisson 분산 락을 Redis 원자적 연산으로 대체

#### 아키텍처 변경

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| 재고 보호 수단 | Redisson RLock + DB PESSIMISTIC_WRITE | Redis `RAtomicLong.addAndGet()` |
| 동시성 모델 | 직렬화 (락 대기) | 비직렬화 (Redis 단일 스레드 원자성) |
| 재고 감소 쿼리 | SELECT FOR UPDATE → entity.stock -= qty | `@Modifying UPDATE WHERE stock >= qty` |

#### FlashPurchaseConsumer 처리 흐름

```
1. Idempotency 체크 (setIfAbsent) → 중복 return
2. Redis RAtomicLong 존재 확인 → 없으면 lazy warm-up (DB read + init lock)
3. addAndGet(-quantity) → remaining < 0이면 복원 후 return (OUT_OF_STOCK)
4. DB decrementStockIfSufficient → 0 반환 시 Redis 복원 후 return (불일치)
5. 구매자 조회 (userRepository.findById)
6. 상품 참조 (productRepository.findById, 락 없이)
7. FlashPurchaseEntity 저장 (CONFIRMED)
8. 메트릭 기록 (success)
```

#### ProductRepository 추가 메서드

```kotlin
@Transactional
@Modifying
@Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
fun decrementStockIfSufficient(@Param("id") id: Long, @Param("quantity") quantity: Int): Int
```

#### 재고 카운터 lazy warm-up

```
"stock:flash:{productId}" 키 미존재 시:
  initLock("init:stock:flash:{productId}") 획득 (1s tryLock, 5s timeout)
  DB findById (락 없이)
  stockCounter.set(product.stock), expire(24h)
  initLock.unlock()
```

---

### 개선 3. Micrometer + Prometheus + Grafana

**목표**: 운영 가시성 확보 — 선착순 구매 결과 실시간 모니터링

#### 의존성

- `app/build.gradle.kts`: `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
- `domain-nearpick/build.gradle.kts`: `io.micrometer:micrometer-core`

#### Actuator 설정 (`application.properties`)

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.tags.application=${spring.application.name}
```

#### 커스텀 메트릭

| 메트릭 | 태그 | 설명 |
|--------|------|------|
| `flash.purchase` | `result=success` | CONFIRMED 저장 성공 |
| `flash.purchase` | `result=duplicate` | idempotency로 차단 |
| `flash.purchase` | `result=out_of_stock` | 재고 부족 |
| `flash.purchase.dlq` | `result=recorded` | DLQ → FAILED 기록 |
| `flash.purchase.dlq` | `result=entity_not_found` | DLQ 처리 불가 |

#### Docker Compose 추가 서비스

- **Prometheus** (`:9090`): `prometheus.yml` scrape config, 7일 retention
- **Grafana** (`:3001`): admin/admin, Prometheus datasource 연결

#### prometheus.yml

```yaml
scrape_configs:
  - job_name: "near-pick"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

---

### 개선 4. Redis 직렬화 수정

**목표**: `JdkSerializationRedisSerializer` → `GenericJackson2JsonRedisSerializer` (JSON 저장)

#### 변경 내용

- `app/build.gradle.kts`: `com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2`, `jackson-datatype-jsr310:2.18.2`
- `RedisConfig.kt`: `redisObjectMapper()` 빈 추가 (Jackson 2.x, Spring Boot 4.x default 3.x와 분리)
  - KotlinModule, JavaTimeModule, `DefaultTyping.EVERYTHING` (Kotlin final class 지원)
- `RedisTemplate` valueSerializer: `GenericJackson2JsonRedisSerializer(redisObjectMapper())`
- `RedisCacheManager` valueSerializer: 동일

---

### 개선 5. Idempotency Key 설계 변경 + DLQ Consumer

#### 5-1. Idempotency Key (일 단위 → 시간 단위)

**문제**: `{userId}-{productId}-{yyyyMMdd}` → 하루 1회 구매만 허용

**변경**: `{userId}-{productId}-{epochSecond/3600}` → 시간 단위 중복 방지

```kotlin
// FlashPurchaseServiceImpl
val idempotencyKey = "$userId-$productId-${Instant.now().epochSecond / 3_600}"
```

#### 5-2. DLQ Consumer (`FlashPurchaseDlqConsumer`)

**목표**: `flash-purchase-dlq` 토픽 처리 — 실패 이벤트 이력 보존

```
1. userId, productId로 User, Product 조회
2. 조회 성공 → FlashPurchaseEntity(status=FAILED) 저장
3. 조회 실패 → 로그 경고 + entity_not_found 메트릭
```

**KafkaListener**: `topics=["flash-purchase-dlq"]`, `groupId="flash-purchase-dlq-cg"`

#### FlashPurchaseStatus.FAILED 추가

```kotlin
enum class FlashPurchaseStatus {
    PENDING, CONFIRMED, CANCELLED, COMPLETED,
    FAILED,  // Consumer 처리 실패 (재고 부족, 상품 없음 등)
}
```

---

## 테스트 전략

| 테스트 | 대상 | 검증 내용 |
|--------|------|-----------|
| `FlashPurchaseConsumerTest` | Consumer | 정상/중복/재고부족/DB불일치/lazy-init/메트릭 (6개) |
| `FlashPurchaseConcurrencyTest` | Consumer | 동일 idempotency key → 1회만 저장 |
| 기존 테스트 유지 | ProductService, ReservationService 등 | 33개 기존 테스트 그대로 통과 |

---

## 파일 변경 목록

| 파일 | 변경 유형 | 설명 |
|------|-----------|------|
| `domain/transaction/FlashPurchaseStatus.kt` | 수정 | FAILED 추가 |
| `domain-nearpick/build.gradle.kts` | 수정 | micrometer-core 추가 |
| `domain-nearpick/.../ProductRepository.kt` | 수정 | decrementStockIfSufficient 추가 |
| `domain-nearpick/.../FlashPurchaseConsumer.kt` | 수정 | Redis 원자적 재고, 메트릭 |
| `domain-nearpick/.../FlashPurchaseServiceImpl.kt` | 수정 | 시간 단위 idempotency key |
| `domain-nearpick/.../FlashPurchaseDlqConsumer.kt` | 신규 | DLQ 처리 |
| `app/build.gradle.kts` | 수정 | actuator, prometheus, jackson2 추가 |
| `app/.../RedisConfig.kt` | 수정 | JSON 직렬화로 교체 |
| `app/.../application.properties` | 수정 | virtual threads, actuator 설정 |
| `docker-compose.yml` | 수정 | Prometheus, Grafana 추가 |
| `prometheus.yml` | 신규 | scrape 설정 |
