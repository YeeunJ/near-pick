# Phase 9 Hardening Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: NearPick
> **Analyst**: gap-detector
> **Date**: 2026-03-11
> **Design Doc**: [phase9-hardening.design.md](../../02-design/features/phase9-hardening.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Phase 9 설계서에서 정의한 5가지 성능/운영 개선 항목이 실제 구현에 정확히 반영되었는지 검증한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/phase9-hardening.design.md`
- **Implementation Files**: 11개 파일 (신규 2, 수정 9)
- **Analysis Date**: 2026-03-11

---

## 2. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 100% | ✅ |
| Architecture Compliance | 100% | ✅ |
| Convention Compliance | 100% | ✅ |
| **Overall** | **100%** | ✅ |

---

## 3. Improvement Item Summary

| # | Improvement | Design Items | Matched | Status |
|---|------------|:------------:|:-------:|:------:|
| 1 | Virtual Threads | 2 | 2 | ✅ Match |
| 2 | Redis Atomic Stock Counter | 6 | 6 | ✅ Match |
| 3 | Micrometer + Prometheus + Grafana | 7 | 7 | ✅ Match |
| 4 | Redis Serialization (JSON) | 4 | 4 | ✅ Match |
| 5 | Idempotency Key + DLQ Consumer | 4 | 4 | ✅ Match |
| **Total** | | **23** | **23** | ✅ |

---

## 4. Detailed Verification

### 4.1 Improvement #1: Virtual Threads

| Design Requirement | Implementation | File:Line | Status |
|--------------------|---------------|-----------|:------:|
| `spring.threads.virtual.enabled=true` in application.properties | Present | `application.properties:13` | ✅ |
| Remove Tomcat thread pool settings from application-local.properties (`server.tomcat.threads.max`, `server.tomcat.threads.min-spare`, `server.tomcat.accept-count`) | None found (grep confirmed 0 matches in non-design files) | `application-local.properties` | ✅ |

### 4.2 Improvement #2: Redis Atomic Stock Counter

| Design Requirement | Implementation | File:Line | Status |
|--------------------|---------------|-----------|:------:|
| `ProductRepository.decrementStockIfSufficient()` with `@Modifying @Query UPDATE WHERE stock >= qty` | Exact match: `@Transactional @Modifying @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")` | `ProductRepository.kt:96-99` | ✅ |
| FlashPurchaseConsumer: idempotency check via `setIfAbsent` | `idempotencyBucket.setIfAbsent("1", Duration.ofDays(1))` | `FlashPurchaseConsumer.kt:32-33` | ✅ |
| FlashPurchaseConsumer: `RAtomicLong.addAndGet(-quantity)`, restore if `remaining < 0` | `stockCounter.addAndGet(-event.quantity.toLong())` with restore on `remaining < 0` | `FlashPurchaseConsumer.kt:42-47` | ✅ |
| FlashPurchaseConsumer: DB `decrementStockIfSufficient`, restore Redis if `updated == 0` | `productRepository.decrementStockIfSufficient(...)` with Redis restore | `FlashPurchaseConsumer.kt:51-56` | ✅ |
| Lazy warm-up: `stock:flash:{productId}` key with init lock, DB read, set + expire 24h | `ensureStockCounter()` method with init lock `init:stock:flash:$productId`, `tryLock(1, 5, SECONDS)`, `stockCounter.set()`, `expire(24h)` | `FlashPurchaseConsumer.kt:89-107` | ✅ |
| Save `FlashPurchaseEntity(status=CONFIRMED)` on success | Exact match | `FlashPurchaseConsumer.kt:69-76` | ✅ |

### 4.3 Improvement #3: Micrometer + Prometheus + Grafana

| Design Requirement | Implementation | File:Line | Status |
|--------------------|---------------|-----------|:------:|
| `app/build.gradle.kts`: `spring-boot-starter-actuator` | `implementation("org.springframework.boot:spring-boot-starter-actuator")` | `app/build.gradle.kts:31` | ✅ |
| `app/build.gradle.kts`: `micrometer-registry-prometheus` | `implementation("io.micrometer:micrometer-registry-prometheus")` | `app/build.gradle.kts:32` | ✅ |
| `domain-nearpick/build.gradle.kts`: `io.micrometer:micrometer-core` | `implementation("io.micrometer:micrometer-core")` | `domain-nearpick/build.gradle.kts:21` | ✅ |
| Actuator settings: `management.endpoints.web.exposure.include=health,info,metrics,prometheus`, `management.endpoint.health.show-details=when-authorized`, `management.metrics.tags.application=${spring.application.name}` | All 3 properties present | `application.properties:16-18` | ✅ |
| Custom metrics: `flash.purchase` counter with tags `result=success/duplicate/out_of_stock`; `flash.purchase.dlq` with tags `result=recorded/entity_not_found` | All 5 metric counters implemented | `FlashPurchaseConsumer.kt:34,45,77` / `FlashPurchaseDlqConsumer.kt:41,53` | ✅ |
| Docker Compose: Prometheus (`:9090`) with `prometheus.yml` mount, 7d retention; Grafana (`:3001`) with admin/admin | Both services present with exact config | `docker-compose.yml:63-85` | ✅ |
| `prometheus.yml`: scrape `near-pick` job, path `/actuator/prometheus`, target `host.docker.internal:8080` | Exact match | `prometheus.yml:1-9` | ✅ |

### 4.4 Improvement #4: Redis Serialization

| Design Requirement | Implementation | File:Line | Status |
|--------------------|---------------|-----------|:------:|
| `app/build.gradle.kts`: `jackson-module-kotlin:2.18.2`, `jackson-datatype-jsr310:2.18.2` | Both present | `app/build.gradle.kts:34-35` | ✅ |
| `redisObjectMapper()` bean: KotlinModule, JavaTimeModule, `DefaultTyping.EVERYTHING` | All registered: `kotlinModule()`, `JavaTimeModule()`, `DefaultTyping.EVERYTHING` with `JsonTypeInfo.As.PROPERTY` | `RedisConfig.kt:36-47` | ✅ |
| `RedisTemplate` valueSerializer: `GenericJackson2JsonRedisSerializer(redisObjectMapper())` | Exact match | `RedisConfig.kt:50-58` | ✅ |
| `RedisCacheManager` valueSerializer: same serializer | `GenericJackson2JsonRedisSerializer(redisObjectMapper())` used in cache config | `RedisConfig.kt:62-81` | ✅ |

### 4.5 Improvement #5: Idempotency Key + DLQ Consumer

| Design Requirement | Implementation | File:Line | Status |
|--------------------|---------------|-----------|:------:|
| Idempotency key: `$userId-$productId-${Instant.now().epochSecond / 3_600}` (hourly) | Exact match | `FlashPurchaseServiceImpl.kt:35` | ✅ |
| `FlashPurchaseStatus.FAILED` added to enum | Present with matching comment | `FlashPurchaseStatus.kt:8` | ✅ |
| `FlashPurchaseDlqConsumer`: `@KafkaListener(topics=["flash-purchase-dlq"], groupId="flash-purchase-dlq-cg")` | Exact match | `FlashPurchaseDlqConsumer.kt:29` | ✅ |
| DLQ logic: user/product lookup, save FAILED if found, log + metric if not found | User/product `findById.orElse(null)`, null check, save `FlashPurchaseEntity(status=FAILED)`, `entity_not_found` metric on missing | `FlashPurchaseDlqConsumer.kt:35-54` | ✅ |

---

## 5. Gaps Found

None. All 23 design requirements are correctly implemented.

---

## 6. File Change List Verification

| Design File | Change Type | Implemented | Status |
|-------------|-------------|:-----------:|:------:|
| `domain/transaction/FlashPurchaseStatus.kt` | Modify | ✅ | ✅ |
| `domain-nearpick/build.gradle.kts` | Modify | ✅ | ✅ |
| `domain-nearpick/.../ProductRepository.kt` | Modify | ✅ | ✅ |
| `domain-nearpick/.../FlashPurchaseConsumer.kt` | Modify | ✅ | ✅ |
| `domain-nearpick/.../FlashPurchaseServiceImpl.kt` | Modify | ✅ | ✅ |
| `domain-nearpick/.../FlashPurchaseDlqConsumer.kt` | New | ✅ | ✅ |
| `app/build.gradle.kts` | Modify | ✅ | ✅ |
| `app/.../RedisConfig.kt` | Modify | ✅ | ✅ |
| `app/.../application.properties` | Modify | ✅ | ✅ |
| `docker-compose.yml` | Modify | ✅ | ✅ |
| `prometheus.yml` | New | ✅ | ✅ |

---

## 7. Match Rate

```
+---------------------------------------------+
|  Overall Match Rate: 100% (23/23)           |
+---------------------------------------------+
|  #1 Virtual Threads:       2/2   (100%)     |
|  #2 Redis Atomic Stock:    6/6   (100%)     |
|  #3 Prometheus + Grafana:  7/7   (100%)     |
|  #4 Redis Serialization:   4/4   (100%)     |
|  #5 Idempotency + DLQ:     4/4   (100%)     |
+---------------------------------------------+
```

---

## 8. Conclusion

Design and implementation match perfectly. All 5 improvement items from the Phase 9 hardening design document are fully and correctly implemented across 11 files. No gaps, missing features, or deviations were found.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-11 | Initial analysis | gap-detector |
