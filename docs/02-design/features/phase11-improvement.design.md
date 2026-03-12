# Design: phase11-improvement

> Phase 11 Gap 보완 + 환경별 Strategy Pattern 분리

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase11-improvement |
| **Phase** | 11 (Post) |
| **목표** | Cache Evict 누락 보완, thumbnailUrl 쿼리 수정, 환경별 의존성 Strategy Pattern 분리 |
| **작성일** | 2026-03-13 |
| **브랜치** | `feature/phase11-product-enhancement` |
| **참조 Plan** | `docs/01-plan/features/phase11-improvement.plan.md` |

---

## 1. Cache Invalidation

### 1.1 문제

`products-detail` 캐시는 `@Cacheable`로 적재되나, 이미지/메뉴 변경 시 `@CacheEvict`가 없어 TTL(60초) 내에 오래된 데이터가 반환될 수 있음.

### 1.2 대상 메서드

| 서비스 | 메서드 | 캐시 키 |
|--------|--------|---------|
| `ProductImageServiceImpl` | `saveImageUrl` | `#productId` |
| `ProductImageServiceImpl` | `deleteImage` | `#productId` |
| `ProductImageServiceImpl` | `reorderImages` | `#productId` |
| `ProductMenuOptionServiceImpl` | `saveMenuOptions` | `#productId` |
| `ProductMenuOptionServiceImpl` | `deleteMenuOptionGroup` | `#productId` |

### 1.3 어노테이션 설계

```kotlin
@Transactional
@CacheEvict(cacheNames = ["products-detail"], key = "#productId")
override fun saveImageUrl(merchantId: Long, productId: Long, request: ProductImageSaveRequest): ProductImageResponse
```

- `cacheNames = ["products-detail"]` — `ProductServiceImpl.getDetail()`이 사용하는 캐시명과 동일
- `key = "#productId"` — 상품 단위 evict (타 상품 캐시 유지)
- 메서드 정상 완료 후 evict 수행 (`beforeInvocation = false` 기본값)

---

## 2. thumbnailUrl (근처 상품 조회)

### 2.1 문제

`GET /api/products/nearby` 응답의 `ProductSummaryResponse.thumbnailUrl`이 항상 `null`.

**원인**: `findNearby` 네이티브 쿼리에 `product_images` JOIN 없음.

### 2.2 쿼리 변경

```sql
-- 추가
LEFT JOIN product_images pi ON pi.product_id = p.id AND pi.display_order = 0

-- SELECT 절 추가
pi.url AS thumbnailUrl
```

`display_order = 0` 기준으로 첫 번째 이미지만 JOIN. 이미지가 없는 상품은 `NULL` 반환.

### 2.3 Projection 변경

```kotlin
// ProductNearbyProjection.kt
interface ProductNearbyProjection {
    // ... 기존 필드 ...
    val thumbnailUrl: String?   // 추가
}
```

### 2.4 Mapper 변경

```kotlin
// ProductMapper.toSummaryResponse()
fun ProductNearbyProjection.toSummaryResponse() = ProductSummaryResponse(
    // ... 기존 필드 ...
    thumbnailUrl = thumbnailUrl,   // 추가
)
```

`ProductSummaryResponse.thumbnailUrl`은 기존에 nullable 필드로 이미 선언됨.

---

## 3. Strategy Pattern — LocationClient

### 3.1 설계 원칙

- 인터페이스는 `domain/` 모듈에 위치 (순수 도메인 의존성)
- `KakaoLocationClient`는 `domain-nearpick/`에서 `LocationClient` 구현
- `NoOpLocationClient`는 `app/src/test/kotlin/`에 위치 (test 컨텍스트 전용)

### 3.2 인터페이스

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/location/LocationClient.kt
package com.nearpick.domain.location

import com.nearpick.domain.location.dto.LocationSearchResult

interface LocationClient {
    fun searchAddress(query: String): List<LocationSearchResult>
}
```

### 3.3 구현체 목록

| 클래스 | 위치 | @Profile | 동작 |
|--------|------|----------|------|
| `KakaoLocationClient` | `domain-nearpick/location/client/` | `!test` | Kakao REST API 실제 호출 |
| `NoOpLocationClient` | `app/src/test/location/` | `test` | 빈 리스트 반환 |

```kotlin
// KakaoLocationClient
@Component
@Profile("!test")
class KakaoLocationClient(...) : LocationClient {
    override fun searchAddress(query: String): List<LocationSearchResult> { ... }
}

// NoOpLocationClient
@Component
@Profile("test")
class NoOpLocationClient : LocationClient {
    override fun searchAddress(query: String): List<LocationSearchResult> = emptyList()
}
```

### 3.4 서비스 변경

```kotlin
// LocationSearchServiceImpl — KakaoLocationClient → LocationClient 인터페이스로 변경
@Service
class LocationSearchServiceImpl(
    private val locationClient: LocationClient,   // 변경
) : LocationSearchService {
    override fun search(query: String) = locationClient.searchAddress(query)
}
```

---

## 4. Strategy Pattern — FlashPurchaseEventProducer

### 4.1 설계 원칙

- `FlashPurchaseRequestEvent`가 `domain-nearpick/`에 있으므로 인터페이스도 동일 모듈에 위치
- `KafkaFlashPurchaseProducer` (기존 `FlashPurchaseProducer` 클래스 파일 교체)
- `NoOpFlashPurchaseEventProducer`는 `domain-nearpick/src/main/kotlin/`에 위치 — `@Profile("test")`로 활성화

> **Note**: `app/`은 `domain-nearpick/`을 `runtimeOnly`로 의존하므로 `app/`에서 인터페이스를 컴파일 참조 불가.
> no-op 구현체도 `domain-nearpick/src/main/`에 배치하되 `@Profile("test")`로 격리.

### 4.2 인터페이스

```kotlin
// domain-nearpick/transaction/messaging/FlashPurchaseEventProducer.kt
interface FlashPurchaseEventProducer {
    fun send(event: FlashPurchaseRequestEvent)
}
```

### 4.3 구현체 목록

| 클래스 | 위치 | @Profile | 동작 |
|--------|------|----------|------|
| `KafkaFlashPurchaseProducer` | `domain-nearpick/transaction/messaging/FlashPurchaseProducer.kt` | `!test` | Kafka 토픽 `flash-purchase-requests`로 전송 |
| `NoOpFlashPurchaseEventProducer` | `domain-nearpick/transaction/messaging/` | `test` | 아무것도 하지 않음 |

```kotlin
// KafkaFlashPurchaseProducer
@Component
@Profile("!test")
class KafkaFlashPurchaseProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : FlashPurchaseEventProducer {
    override fun send(event: FlashPurchaseRequestEvent) {
        kafkaTemplate.send("flash-purchase-requests", event.productId.toString(), event)
    }
}

// NoOpFlashPurchaseEventProducer
@Component
@Profile("test")
class NoOpFlashPurchaseEventProducer : FlashPurchaseEventProducer {
    override fun send(event: FlashPurchaseRequestEvent) { /* no-op */ }
}
```

### 4.4 Consumer 프로필 격리

```kotlin
@Component
@Profile("!test")   // 추가
class FlashPurchaseConsumer(...) { ... }

@Component
@Profile("!test")   // 추가
class FlashPurchaseDlqConsumer(...) { ... }
```

`@KafkaListener`는 Kafka 브로커 연결을 시도하므로 test 프로필에서 등록하지 않아야 함.

### 4.5 서비스 변경

```kotlin
// FlashPurchaseServiceImpl — FlashPurchaseProducer → FlashPurchaseEventProducer 인터페이스로 변경
@Service
class FlashPurchaseServiceImpl(
    private val producer: FlashPurchaseEventProducer,   // 변경
    ...
)
```

---

## 5. Strategy Pattern — ImageStorageService

### 5.1 프로필 전략 요약

| 프로필 | 구현체 | 저장 방식 |
|--------|--------|----------|
| `local` | `LocalImageStorageService` | 로컬 파일 시스템 (`./uploads/`) |
| `test` | `LocalImageStorageService` | 로컬 파일 시스템 (URL만 반환, 실제 저장 선택적) |
| 그 외 (prod) | `S3ImageStorageService` | AWS S3 Presigned URL |

### 5.2 변경 사항

```kotlin
// LocalImageStorageService — @Profile 확장
@Service
@Profile("local | test")   // "local" → "local | test"
class LocalImageStorageService(...) : ImageStorageService { ... }
```

`NoOpImageStorageService` 삭제 — `LocalImageStorageService`가 test도 커버.

### 5.3 URL 패턴 (local + test 동일)

```
presignedPutUrl : http://localhost:{port}/local-upload/{s3Key}
publicUrl       : http://localhost:{port}/local-upload/{s3Key}
```

---

## 6. Profile Matrix (전체)

| 기능 | local | test | prod |
|------|-------|------|------|
| `ImageStorageService` | `LocalImageStorageService` | `LocalImageStorageService` | `S3ImageStorageService` |
| `LocationClient` | `KakaoLocationClient` | `NoOpLocationClient` | `KakaoLocationClient` |
| `FlashPurchaseEventProducer` | `KafkaFlashPurchaseProducer` | `NoOpFlashPurchaseEventProducer` | `KafkaFlashPurchaseProducer` |
| `FlashPurchaseConsumer` | 활성 (`@KafkaListener` 등록) | 비활성 | 활성 |
| `FlashPurchaseDlqConsumer` | 활성 | 비활성 | 활성 |

---

## 7. 모듈 배치 요약

```
domain/
  location/
    LocationClient.kt                          ← 인터페이스 (신규)

domain-nearpick/
  location/client/
    KakaoLocationClient.kt                     ← @Profile("!test") 추가, LocationClient 구현
  transaction/messaging/
    FlashPurchaseEventProducer.kt              ← 인터페이스 (신규)
    FlashPurchaseProducer.kt                   ← KafkaFlashPurchaseProducer로 교체, @Profile("!test")
    NoOpFlashPurchaseEventProducer.kt          ← no-op 구현체 (신규)
    FlashPurchaseConsumer.kt                   ← @Profile("!test") 추가
    FlashPurchaseDlqConsumer.kt               ← @Profile("!test") 추가
  product/service/
    ProductImageServiceImpl.kt                 ← @CacheEvict 추가
    ProductMenuOptionServiceImpl.kt            ← @CacheEvict 추가
  product/repository/
    ProductRepository.kt                       ← findNearby LEFT JOIN 추가
    ProductNearbyProjection.kt                 ← thumbnailUrl 필드 추가
  product/mapper/
    ProductMapper.kt                           ← thumbnailUrl 매핑 추가

app/src/main/
  storage/
    LocalImageStorageService.kt                ← @Profile("local | test")로 확장

app/src/test/
  location/
    NoOpLocationClient.kt                      ← no-op LocationClient (신규)
  storage/
    NoOpImageStorageService.kt                 ← 삭제 (LocalImageStorageService로 통합)
```

---

## 8. 테스트 영향 범위

| 테스트 | 변경 내용 |
|--------|----------|
| `LocationSearchServiceImplTest` | `@Mock KakaoLocationClient` → `@Mock LocationClient`로 변경 |
| `FlashPurchaseServiceImplTest` | `@Mock FlashPurchaseProducer` → `@Mock FlashPurchaseEventProducer`로 변경 |
| 컨트롤러 테스트 (`@ActiveProfiles("test")`) | `NoOpLocationClient`, `NoOpFlashPurchaseEventProducer`, `LocalImageStorageService` 자동 활성화 |
