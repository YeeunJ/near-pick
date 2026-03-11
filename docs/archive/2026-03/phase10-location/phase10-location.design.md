# Design: phase10-location

> Phase 10 — 위치 & 지도 서비스

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase10-location |
| **Phase** | 10 |
| **목표** | 현재 위치 갱신 + 저장 위치 CRUD + 카카오 주소 검색 + nearby locationSource |
| **작성일** | 2026-03-11 |
| **브랜치** | `feature/phase10-location` |
| **참조 Plan** | `docs/01-plan/features/phase10-location.plan.md` |

---

## 1. API 명세

### 1.1 현재 위치 갱신 (Consumer)

```
PATCH /consumers/me/location
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Request Body**
```json
{
  "lat": 37.5665,
  "lng": 126.9780
}
```

**Response**
- `204 No Content` (성공)
- `400 Bad Request` — lat/lng 범위 위반
- `401 Unauthorized` — 미인증
- `403 Forbidden` — CONSUMER 이외 role

---

### 1.2 저장 위치 목록 조회

```
GET /consumers/me/locations
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Response 200**
```json
[
  {
    "id": 1,
    "label": "집",
    "lat": 37.5665,
    "lng": 126.9780,
    "isDefault": true,
    "createdAt": "2026-03-11T10:00:00"
  }
]
```

---

### 1.3 저장 위치 추가

```
POST /consumers/me/locations
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Request Body**
```json
{
  "label": "직장",
  "lat": 37.5172,
  "lng": 127.0473,
  "isDefault": false
}
```

**Response**
- `201 Created` — `SavedLocationResponse`
- `400 Bad Request` — 유효성 실패 or 5개 초과 (`SAVED_LOCATION_LIMIT_EXCEEDED`)

---

### 1.4 저장 위치 수정

```
PUT /consumers/me/locations/{id}
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Request Body** (모두 optional, 하나 이상 필수)
```json
{
  "label": "본가",
  "isDefault": true
}
```

**Response**
- `200 OK` — `SavedLocationResponse`
- `404 Not Found` — `SAVED_LOCATION_NOT_FOUND`

---

### 1.5 저장 위치 삭제

```
DELETE /consumers/me/locations/{id}
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Response**
- `204 No Content`
- `404 Not Found` — `SAVED_LOCATION_NOT_FOUND`

---

### 1.6 기본 위치 지정

```
PATCH /consumers/me/locations/{id}/default
Authorization: Bearer {JWT}  (CONSUMER role 필수)
```

**Response**
- `200 OK` — `SavedLocationResponse`
- `404 Not Found` — `SAVED_LOCATION_NOT_FOUND`

---

### 1.7 주소 검색 (카카오 연동)

```
GET /location/search?query=서울시 강남구
Authorization: Bearer {JWT}  (CONSUMER 또는 MERCHANT)
```

**Response 200** (최대 5건)
```json
[
  {
    "address": "서울 강남구 테헤란로 1",
    "lat": 37.4979,
    "lng": 127.0276
  }
]
```

**Error**
- `503 Service Unavailable` — `EXTERNAL_API_UNAVAILABLE` (Kakao API 타임아웃/오류)

---

### 1.8 nearby locationSource 파라미터 추가 (기존 수정)

```
GET /products/nearby?locationSource=current
GET /products/nearby?locationSource=saved&savedLocationId=1
GET /products/nearby?lat=37.5&lng=127.0  (기존 방식 — DIRECT 기본값)
```

`locationSource` 값에 따른 동작:
| 값 | lat/lng 결정 방식 |
|----|------------------|
| `DIRECT` (기본) | 요청 파라미터의 lat/lng 직접 사용 (필수) |
| `CURRENT` | ConsumerProfile.currentLat/currentLng 사용 (CONSUMER only) |
| `SAVED` | SavedLocation[savedLocationId].lat/lng 사용 (CONSUMER only) |

---

## 2. DTO 설계

### 2.1 domain 모듈 신규 DTOs

**파일**: `domain/src/main/kotlin/com/nearpick/domain/location/dto/LocationDtos.kt`

```kotlin
package com.nearpick.domain.location.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class UpdateCurrentLocationRequest(
    @field:NotNull val lat: BigDecimal,
    @field:NotNull val lng: BigDecimal,
)

data class CreateSavedLocationRequest(
    @field:NotBlank @field:Size(max = 50) val label: String,
    @field:NotNull val lat: BigDecimal,
    @field:NotNull val lng: BigDecimal,
    val isDefault: Boolean = false,
)

data class UpdateSavedLocationRequest(
    @field:Size(max = 50) val label: String? = null,
    val isDefault: Boolean? = null,
)

data class SavedLocationResponse(
    val id: Long,
    val label: String,
    val lat: BigDecimal,
    val lng: BigDecimal,
    val isDefault: Boolean,
    val createdAt: LocalDateTime,
)

data class LocationSearchResult(
    val address: String,
    val lat: BigDecimal,
    val lng: BigDecimal,
)
```

---

### 2.2 LocationSource enum

**파일**: `domain/src/main/kotlin/com/nearpick/domain/location/LocationSource.kt`

```kotlin
package com.nearpick.domain.location

enum class LocationSource {
    DIRECT,   // lat/lng 직접 전달 (기본값)
    CURRENT,  // ConsumerProfile.currentLat/currentLng
    SAVED,    // SavedLocation[savedLocationId]
}
```

---

### 2.3 ProductNearbyRequest 수정

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/dto/ProductDtos.kt`

```kotlin
data class ProductNearbyRequest(
    val lat: BigDecimal? = null,
    val lng: BigDecimal? = null,
    @field:Positive @field:Max(50) val radius: Double = 5.0,
    val sort: SortType = SortType.POPULARITY,
    @field:Min(0) val page: Int = 0,
    @field:Positive @field:Max(100) val size: Int = 20,
    val locationSource: LocationSource = LocationSource.DIRECT,
    val savedLocationId: Long? = null,
) {
    @AssertTrue(message = "lat and lng are required when locationSource is DIRECT")
    fun isLocationValid(): Boolean {
        if (locationSource == LocationSource.DIRECT) return lat != null && lng != null
        if (locationSource == LocationSource.SAVED) return savedLocationId != null
        return true  // CURRENT
    }
}
```

---

## 3. 서비스 인터페이스 설계

### 3.1 ConsumerLocationService (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/location/ConsumerLocationService.kt`

```kotlin
package com.nearpick.domain.location

import com.nearpick.domain.location.dto.UpdateCurrentLocationRequest

interface ConsumerLocationService {
    fun updateCurrentLocation(userId: Long, request: UpdateCurrentLocationRequest)
}
```

---

### 3.2 SavedLocationService (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/location/SavedLocationService.kt`

```kotlin
package com.nearpick.domain.location

import com.nearpick.domain.location.dto.*

interface SavedLocationService {
    fun getLocations(userId: Long): List<SavedLocationResponse>
    fun addLocation(userId: Long, request: CreateSavedLocationRequest): SavedLocationResponse
    fun updateLocation(userId: Long, locationId: Long, request: UpdateSavedLocationRequest): SavedLocationResponse
    fun deleteLocation(userId: Long, locationId: Long)
    fun setDefault(userId: Long, locationId: Long): SavedLocationResponse
    fun getLocation(userId: Long, locationId: Long): SavedLocationResponse
}
```

---

### 3.3 LocationSearchService (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/location/LocationSearchService.kt`

```kotlin
package com.nearpick.domain.location

import com.nearpick.domain.location.dto.LocationSearchResult

interface LocationSearchService {
    fun search(query: String): List<LocationSearchResult>
}
```

---

## 4. 엔티티 설계

### 4.1 SavedLocationEntity

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/entity/SavedLocationEntity.kt`

```kotlin
package com.nearpick.nearpick.location.entity

import com.nearpick.nearpick.user.entity.ConsumerProfileEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "saved_locations",
    indexes = [Index(name = "idx_saved_location_consumer_id", columnList = "consumer_id")]
)
class SavedLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", nullable = false)
    val consumer: ConsumerProfileEntity,

    @Column(nullable = false, length = 50)
    var label: String,

    @Column(precision = 10, scale = 7, nullable = false)
    var lat: BigDecimal,

    @Column(precision = 10, scale = 7, nullable = false)
    var lng: BigDecimal,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

**제약 조건**:
- `consumer_id`당 최대 5개 (서비스 레이어 검증)
- `isDefault = true`는 consumer당 1개만 허용 → `setDefault()` 시 기존 default 자동 해제

---

## 5. Repository 설계

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/repository/SavedLocationRepository.kt`

```kotlin
package com.nearpick.nearpick.location.repository

import com.nearpick.nearpick.location.entity.SavedLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SavedLocationRepository : JpaRepository<SavedLocationEntity, Long> {
    fun findAllByConsumerUserId(userId: Long): List<SavedLocationEntity>
    fun countByConsumerUserId(userId: Long): Long
    fun findByIdAndConsumerUserId(id: Long, userId: Long): SavedLocationEntity?

    @Modifying
    @Query("UPDATE SavedLocationEntity s SET s.isDefault = false WHERE s.consumer.userId = :userId AND s.id != :exceptId")
    fun clearDefaultExcept(userId: Long, exceptId: Long)

    @Modifying
    @Query("UPDATE SavedLocationEntity s SET s.isDefault = false WHERE s.consumer.userId = :userId")
    fun clearAllDefault(userId: Long)
}
```

**ConsumerProfileRepository 추가 쿼리**

```kotlin
// 기존 ConsumerProfileRepository에 추가
@Modifying
@Query("UPDATE ConsumerProfileEntity c SET c.currentLat = :lat, c.currentLng = :lng, c.updatedAt = :now WHERE c.userId = :userId")
fun updateCurrentLocation(userId: Long, lat: BigDecimal, lng: BigDecimal, now: LocalDateTime): Int
```

---

## 6. 서비스 구현체 설계

### 6.1 ConsumerLocationServiceImpl

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/service/ConsumerLocationServiceImpl.kt`

핵심 로직:
```
1. consumerProfileRepository.updateCurrentLocation(userId, lat, lng, LocalDateTime.now())
2. 결과 rows == 0 → CONSUMER_NOT_FOUND 예외
```

---

### 6.2 SavedLocationServiceImpl

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/service/SavedLocationServiceImpl.kt`

핵심 로직:
- `addLocation`: count check → 5개 초과 시 `SAVED_LOCATION_LIMIT_EXCEEDED`; isDefault=true면 기존 default 해제
- `setDefault`: `clearDefaultExcept(userId, locationId)` 후 해당 entity `isDefault = true`
- `deleteLocation`: consumer 소유권 검증 후 삭제
- `updateLocation`: label, isDefault 각각 null-safe 업데이트; isDefault=true면 기존 default 해제

---

### 6.3 KakaoLocationClient

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/client/KakaoLocationClient.kt`

```kotlin
@Component
class KakaoLocationClient(
    @Value("\${kakao.rest-api-key:}") private val apiKey: String,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .defaultHeader("Authorization", "KakaoAK $apiKey")
        .build()

    fun searchAddress(query: String): List<LocationSearchResult> {
        // GET /v2/local/search/address.json?query={query}&size=5
        // 타임아웃: 3초
        // 실패 시 EXTERNAL_API_UNAVAILABLE 예외
    }
}
```

카카오 API 응답 파싱:
```
documents[].address_name → LocationSearchResult.address
documents[].y            → lat (String → BigDecimal)
documents[].x            → lng (String → BigDecimal)
```
최대 5건 반환.

---

### 6.4 LocationSearchServiceImpl

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/location/service/LocationSearchServiceImpl.kt`

```
kakaoLocationClient.searchAddress(query)
→ 결과 반환 (빈 리스트도 정상)
→ KakaoLocationClient에서 예외 발생 시 EXTERNAL_API_UNAVAILABLE 전파
```

---

### 6.5 ProductServiceImpl 수정

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/service/ProductServiceImpl.kt`

`getNearby()` 메서드 내부 위치 결정 로직:
```
when (request.locationSource) {
    DIRECT  → request.lat, request.lng (이미 @AssertTrue로 검증됨)
    CURRENT → consumerProfileRepository.findById(userId).currentLat/Lng
              (null이면 LOCATION_NOT_SET 예외)
    SAVED   → savedLocationRepository.findByIdAndConsumerUserId(savedLocationId, userId)
              (없으면 SAVED_LOCATION_NOT_FOUND 예외)
}
```

CONSUMER 이외 role이 CURRENT/SAVED 사용 시 → `DIRECT` 강제 (혹은 `FORBIDDEN` 예외, 컨트롤러 레이어에서 처리).

---

## 7. 컨트롤러 설계

### 7.1 ConsumerLocationController (신규)

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/ConsumerLocationController.kt`

```kotlin
@RestController
@RequestMapping("/consumers/me")
class ConsumerLocationController(
    private val consumerLocationService: ConsumerLocationService,
    private val savedLocationService: SavedLocationService,
) {
    @PatchMapping("/location")
    @PreAuthorize("hasRole('CONSUMER')")
    fun updateLocation(@AuthenticationPrincipal userId: Long,
                       @Valid @RequestBody request: UpdateCurrentLocationRequest): ResponseEntity<Void>

    @GetMapping("/locations")
    @PreAuthorize("hasRole('CONSUMER')")
    fun getLocations(@AuthenticationPrincipal userId: Long): ResponseEntity<ApiResponse<List<SavedLocationResponse>>>

    @PostMapping("/locations")
    @PreAuthorize("hasRole('CONSUMER')")
    fun addLocation(@AuthenticationPrincipal userId: Long,
                    @Valid @RequestBody request: CreateSavedLocationRequest): ResponseEntity<ApiResponse<SavedLocationResponse>>

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasRole('CONSUMER')")
    fun updateLocation(@AuthenticationPrincipal userId: Long,
                       @PathVariable id: Long,
                       @Valid @RequestBody request: UpdateSavedLocationRequest): ResponseEntity<ApiResponse<SavedLocationResponse>>

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasRole('CONSUMER')")
    fun deleteLocation(@AuthenticationPrincipal userId: Long,
                       @PathVariable id: Long): ResponseEntity<Void>

    @PatchMapping("/locations/{id}/default")
    @PreAuthorize("hasRole('CONSUMER')")
    fun setDefault(@AuthenticationPrincipal userId: Long,
                   @PathVariable id: Long): ResponseEntity<ApiResponse<SavedLocationResponse>>
}
```

---

### 7.2 LocationController (신규)

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/LocationController.kt`

```kotlin
@RestController
@RequestMapping("/location")
class LocationController(
    private val locationSearchService: LocationSearchService,
) {
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CONSUMER', 'MERCHANT')")
    fun search(@RequestParam query: String): ResponseEntity<ApiResponse<List<LocationSearchResult>>>
}
```

---

### 7.3 ProductController 수정

`getNearby()` 파라미터 변경:
```kotlin
@GetMapping("/nearby")
fun getNearby(
    @AuthenticationPrincipal userId: Long?,
    @Valid @ModelAttribute request: ProductNearbyRequest,
): ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>>
```
- `lat`, `lng`를 `@RequestParam(required = false)`로 처리 (DTO에서 `@AssertTrue` 검증)
- `locationSource`, `savedLocationId` 파라미터 추가

---

## 8. ErrorCode 추가

**파일**: `common/src/main/kotlin/com/nearpick/common/ErrorCode.kt`

```kotlin
// 기존 enum에 추가
SAVED_LOCATION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "저장 위치는 최대 5개까지 등록 가능합니다."),
SAVED_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "저장 위치를 찾을 수 없습니다."),
LOCATION_NOT_SET(HttpStatus.BAD_REQUEST, "현재 위치가 설정되지 않았습니다."),
EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "외부 API를 사용할 수 없습니다."),
```

---

## 9. Flyway 마이그레이션

**파일**: `app/src/main/resources/db/migration/V4__add_saved_locations.sql`

```sql
CREATE TABLE saved_locations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    consumer_id BIGINT       NOT NULL,
    label       VARCHAR(50)  NOT NULL,
    lat         DECIMAL(10, 7) NOT NULL,
    lng         DECIMAL(10, 7) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_saved_location_consumer
        FOREIGN KEY (consumer_id) REFERENCES consumer_profiles (user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_saved_location_consumer_id ON saved_locations (consumer_id);
```

---

## 10. 설정 추가

**`application-local.properties`** (gitignored, 로컬 전용):
```properties
kakao.rest-api-key=YOUR_KAKAO_REST_API_KEY
```

**`application.properties`** (기본값 — 키 없으면 주소 검색 불가):
```properties
kakao.rest-api-key=
```

---

## 11. 테스트 전략

### 11.1 단위 테스트 (8개 이상)

| 테스트 클래스 | 케이스 |
|---------------|--------|
| `SavedLocationServiceImplTest` | 목록 조회, 추가 성공, 5개 초과 실패, 수정, 삭제, 기본 위치 지정 |
| `ConsumerLocationServiceImplTest` | 현재 위치 갱신 성공, consumer 없음 실패 |
| `LocationSearchServiceImplTest` | 검색 성공 (결과 있음), 카카오 API 실패 시 예외 |

### 11.2 테스트 파일 위치

```
domain-nearpick/src/test/kotlin/com/nearpick/nearpick/location/
  ├── service/SavedLocationServiceImplTest.kt
  ├── service/ConsumerLocationServiceImplTest.kt
  └── service/LocationSearchServiceImplTest.kt
```

---

## 12. 파일 변경 목록

| 모듈 | 파일 | 변경 유형 |
|------|------|-----------|
| `common` | `ErrorCode.kt` | 수정 (4개 에러코드 추가) |
| `domain` | `location/LocationSource.kt` | 신규 |
| `domain` | `location/ConsumerLocationService.kt` | 신규 |
| `domain` | `location/SavedLocationService.kt` | 신규 |
| `domain` | `location/LocationSearchService.kt` | 신규 |
| `domain` | `location/dto/LocationDtos.kt` | 신규 |
| `domain` | `product/dto/ProductDtos.kt` | 수정 (ProductNearbyRequest) |
| `domain-nearpick` | `location/entity/SavedLocationEntity.kt` | 신규 |
| `domain-nearpick` | `location/repository/SavedLocationRepository.kt` | 신규 |
| `domain-nearpick` | `location/service/ConsumerLocationServiceImpl.kt` | 신규 |
| `domain-nearpick` | `location/service/SavedLocationServiceImpl.kt` | 신규 |
| `domain-nearpick` | `location/client/KakaoLocationClient.kt` | 신규 |
| `domain-nearpick` | `location/service/LocationSearchServiceImpl.kt` | 신규 |
| `domain-nearpick` | `user/repository/ConsumerProfileRepository.kt` | 수정 (updateCurrentLocation 추가) |
| `domain-nearpick` | `product/service/ProductServiceImpl.kt` | 수정 (locationSource 처리) |
| `app` | `controller/ConsumerLocationController.kt` | 신규 |
| `app` | `controller/LocationController.kt` | 신규 |
| `app` | `controller/ProductController.kt` | 수정 (getNearby 파라미터) |
| `app` | `db/migration/V4__add_saved_locations.sql` | 신규 |
| `app` | `application-local.properties` | 수정 (kakao.rest-api-key) |

---

## 13. 구현 순서

```
1. ErrorCode 추가 (common)
2. LocationSource enum + LocationDtos + 서비스 인터페이스 (domain)
3. ProductNearbyRequest 수정 (domain)
4. V4__add_saved_locations.sql + SavedLocationEntity (domain-nearpick)
5. SavedLocationRepository + ConsumerProfileRepository 수정 (domain-nearpick)
6. ConsumerLocationServiceImpl + SavedLocationServiceImpl (domain-nearpick)
7. KakaoLocationClient + LocationSearchServiceImpl (domain-nearpick)
8. ProductServiceImpl 수정 (domain-nearpick)
9. ConsumerLocationController + LocationController + ProductController 수정 (app)
10. 단위 테스트 작성 (8개 이상)
11. ./gradlew build + :app:bootRun 확인
```

---

## 14. 성공 기준

| 항목 | 기준 |
|------|------|
| API 응답 정상 | 현재 위치 갱신 204, 저장 위치 CRUD 200/201/204, 주소 검색 200 |
| 최대 5개 제한 | 6번째 추가 시 `SAVED_LOCATION_LIMIT_EXCEEDED` 400 반환 |
| 기본 위치 단일성 | `setDefault` 호출 후 해당 consumer에 `is_default=true` 항상 1개 |
| nearby locationSource | `locationSource=current` + `locationSource=saved&savedLocationId=1` 정상 동작 |
| 단위 테스트 | 8개 이상 통과 |
| Gap Analysis | Match Rate ≥ 90% |
