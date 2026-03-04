# [Design] Phase 8 — Code Review & Quality

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase8-review |
| Phase | Design |
| 작성일 | 2026-03-05 |
| 참조 Plan | `docs/01-plan/features/phase8-review.plan.md` |

---

## 1. 코드 리뷰 결과 — 발견 이슈

전체 코드베이스 리뷰 결과, 아키텍처 위반은 없으나 API 일관성/인덱스/예외 처리에서 수정이 필요한 항목이 발견됐다.

### 심각도 분류

| 심각도 | 기준 |
|:------:|------|
| **P1** | 기능 오작동 또는 잘못된 HTTP 응답 |
| **P2** | 성능 리스크 또는 일관성 위반 |
| **P3** | 코드 품질 개선 (Minor) |

---

## 2. P1 — 기능 오작동 / 잘못된 응답

### Issue #1 — `AdminController.withdrawUser()` 응답 불일치

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/AdminController.kt`

**현재 코드**:
```kotlin
@DeleteMapping("/users/{userId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun withdrawUser(@PathVariable userId: Long) {
    adminService.withdrawUser(userId)  // 반환값 UserSummary 버려짐
}
```

**문제**: `adminService.withdrawUser()` 가 `UserSummary`를 반환하지만 컨트롤러에서 버려짐. 204 No Content는 응답 바디가 없어야 함. 서비스 반환 타입과 Controller가 불일치.

**수정 방향**: 204는 DELETE에서 관용적으로 허용되지만, 현재 다른 `PATCH` 작업들(suspend, forceClose)은 모두 200 + `ApiResponse<T>`를 반환한다. 일관성을 위해 withdrawUser도 200으로 통일.

```kotlin
// 수정 후
@DeleteMapping("/users/{userId}")
fun withdrawUser(@PathVariable userId: Long) =
    ApiResponse.success(adminService.withdrawUser(userId))
```

---

### Issue #2 — `WishlistServiceImpl.remove()` 잘못된 ErrorCode

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/transaction/service/WishlistServiceImpl.kt:34`

**현재 코드**:
```kotlin
override fun remove(userId: Long, productId: Long) {
    val wishlist = wishlistRepository.findByUser_IdAndProduct_Id(userId, productId)
        ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)  // ❌ 잘못된 ErrorCode
    wishlistRepository.delete(wishlist)
}
```

**문제**: 찜 항목이 없을 때 `PRODUCT_NOT_FOUND`(상품 없음)을 던짐. 실제 상품이 존재하더라도 찜을 안 한 경우 동일한 에러가 반환됨 → 클라이언트 혼란.

**수정 방향**: `RESOURCE_NOT_FOUND` 사용 (이미 `ErrorCode`에 정의됨):
```kotlin
?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
```

---

### Issue #3 — `GlobalExceptionHandler` 미처리 예외 → 500 반환

**파일**: `app/src/main/kotlin/com/nearpick/app/config/GlobalExceptionHandler.kt`

**현재**: `HttpRequestMethodNotAllowedException`(405), `MissingServletRequestParameterException`(400) 등 Spring MVC 표준 예외를 처리하는 핸들러 없음 → 모두 500으로 떨어짐.

**추가할 핸들러**:
```kotlin
@ExceptionHandler(HttpRequestMethodNotAllowedException::class)
fun handleMethodNotAllowed(e: HttpRequestMethodNotAllowedException): ResponseEntity<ApiResponse<Nothing>> =
    ResponseEntity
        .status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(ApiResponse.error("Method not allowed: ${e.method}"))

@ExceptionHandler(MissingServletRequestParameterException::class)
fun handleMissingParam(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Nothing>> =
    ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Missing required parameter: ${e.parameterName}"))
```

**import 추가 필요**:
```kotlin
import org.springframework.web.HttpRequestMethodNotAllowedException
import org.springframework.web.bind.MissingServletRequestParameterException
```

---

## 3. P2 — 성능 리스크 / API 일관성

### Issue #4 — `ReservationEntity`, `FlashPurchaseEntity` — `product_id` 인덱스 누락

**파일**:
- `domain-nearpick/.../transaction/entity/ReservationEntity.kt`
- `domain-nearpick/.../transaction/entity/FlashPurchaseEntity.kt`

**현재**: 두 Entity 모두 `user_id` 인덱스만 있고, `product_id` 인덱스 없음.

**문제**: `countByProduct_Id()` (ProductServiceImpl.getDetail에서 사용) 시 전체 테이블 스캔. 또한 `findByMerchantIdAndStatus`는 reservation → product → merchant 조인을 타므로 `product_id`에 인덱스가 없으면 성능 저하.

**수정**:
```kotlin
// ReservationEntity
@Table(
    name = "reservations",
    indexes = [
        Index(name = "idx_reservations_user", columnList = "user_id"),
        Index(name = "idx_reservations_product", columnList = "product_id"),  // 추가
        Index(name = "idx_reservations_status", columnList = "status"),        // 추가
    ]
)

// FlashPurchaseEntity
@Table(
    name = "flash_purchases",
    indexes = [
        Index(name = "idx_flash_purchases_user", columnList = "user_id"),
        Index(name = "idx_flash_purchases_product", columnList = "product_id"),  // 추가
    ]
)
```

> **주의**: Entity `@Index` 변경은 Flyway 마이그레이션 SQL도 함께 수정 필요 (Phase 9 배포 전 `V3__add_indexes.sql` 추가).

---

### Issue #5 — `WishlistController.add()` 응답 — 비표준 `mapOf()` 반환

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/WishlistController.kt:38`

**현재**:
```kotlin
ApiResponse.success(mapOf("wishlistId" to wishlistService.add(userId, request.productId)))
```

**문제**: 다른 endpoint는 모두 도메인 DTO를 반환하는데, 여기만 임시 `Map` 사용. 타입 안정성 없음.

**수정**: `domain` 모듈에 `WishlistAddResponse` DTO 추가:

```kotlin
// domain/src/main/kotlin/com/nearpick/domain/transaction/dto/TransactionDtos.kt 에 추가
data class WishlistAddResponse(val wishlistId: Long)
```

```kotlin
// WishlistController.add() 수정
fun add(...) = ApiResponse.success(WishlistAddResponse(wishlistService.add(userId, request.productId)))
```

---

### Issue #6 — `WishlistServiceImpl.getMyWishlists()` 페이지네이션 없음

**파일**: `domain-nearpick/.../transaction/service/WishlistServiceImpl.kt:41`

**현재**:
```kotlin
override fun getMyWishlists(userId: Long): List<WishlistItem> =
    wishlistRepository.findAllByUser_Id(userId).map { it.toItem() }
```

**문제**: 찜 목록이 무제한으로 반환됨. 사용자가 대량 찜 추가 시 메모리 + 응답 크기 위험.

**수정 방향**: 페이지네이션 추가 (다만 찜 목록은 UX상 페이지 없이 전체 보여주는 경우가 많으므로 **최대 200개 제한**으로 타협):

```kotlin
// WishlistRepository에 추가
@Query("SELECT w FROM WishlistEntity w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
fun findTop200ByUser_Id(@Param("userId") userId: Long): List<WishlistEntity>

// WishlistServiceImpl 수정
override fun getMyWishlists(userId: Long): List<WishlistItem> =
    wishlistRepository.findTop200ByUser_Id(userId).map { it.toItem() }
```

> **의사결정**: Page 반환으로 바꾸면 WishlistService interface, WishlistController, 테스트까지 연쇄 변경 필요. 현 단계에서는 최대 200개 제한으로 최소 변경.

---

### Issue #7 — `@Valid @RequestBody` vs `@RequestBody @Valid` 어노테이션 순서 불일치

**현재 혼용**:
- `AuthController`, `WishlistController`, `ReservationController`: `@RequestBody @Valid`
- `ProductController`, `FlashPurchaseController`: `@Valid @RequestBody`

**수정**: 전체 `@RequestBody @Valid` 로 통일 (Kotlin 컴파일러가 어느 쪽이든 동일하게 처리하지만 가독성 일관성).

---

## 4. P3 — 테스트 보완

### Issue #8 — `WishlistServiceImpl` 서비스 테스트 없음

**현재 서비스 테스트**:
- `AuthServiceImplTest` ✅
- `MerchantServiceImplTest` ✅
- `ReservationServiceImplTest` ✅
- `FlashPurchaseServiceImplTest` ✅
- `WishlistServiceImpl` ❌ — 없음
- `ProductServiceImpl` ❌ — 없음

**추가할 파일**: `domain-nearpick/src/test/kotlin/com/nearpick/nearpick/transaction/service/WishlistServiceImplTest.kt`

**테스트 케이스**:
```kotlin
@ExtendWith(MockitoExtension::class)
class WishlistServiceImplTest {
    // add() — 성공
    // add() — 이미 찜한 경우 → ALREADY_WISHLISTED
    // add() — 상품 없음 → PRODUCT_NOT_FOUND
    // remove() — 성공
    // remove() — 찜 없음 → RESOURCE_NOT_FOUND (Issue #2 수정 후)
    // getMyWishlists() — 목록 반환
}
```

---

### Issue #9 — `RateLimitFilter` 단위 테스트 없음

**현재**: `RateLimitFilter`는 구현 완료됐으나 단위 테스트 없음.

**추가할 파일**: `app/src/test/kotlin/com/nearpick/app/config/RateLimitFilterTest.kt`

**테스트 케이스**:
```kotlin
class RateLimitFilterTest {
    // 정상 요청 → chain.doFilter() 호출
    // 제한 초과 → 429 응답 + JSON body
    // /api/auth/login POST → loginBucket 사용 (10/min)
    // 일반 API → apiBucket 사용 (200/min)
    // X-Forwarded-For 헤더 있을 때 IP 추출 정확성
}
```

---

## 5. 알려진 한계 (Known Limitations — 수정 보류)

| 항목 | 현황 | 보류 이유 |
|------|------|---------|
| `ProductServiceImpl.getDetail()` count 3회 쿼리 | wishlist/reservation/purchase 각 1회 | 트래픽 낮음, 최적화 효과 미미 |
| `RateLimitFilter` ConcurrentHashMap 무제한 성장 | IP별 Bucket 무한 축적 | Phase 9 이후 Redis 전환 시 해결 |
| Flyway 마이그레이션 V3 (인덱스 추가) | Entity @Index는 추가하나 SQL은 Phase 9에서 작성 | DB 스키마 변경은 배포 시 함께 처리 |

---

## 6. 수정 파일 목록

| 파일 | 변경 유형 | Issue |
|------|---------|-------|
| `app/.../controller/AdminController.kt` | 수정 — withdrawUser 200 응답 | #1 |
| `domain-nearpick/.../service/WishlistServiceImpl.kt` | 수정 — ErrorCode 교정, getMyWishlists 제한 | #2, #6 |
| `app/.../config/GlobalExceptionHandler.kt` | 수정 — 405/400 핸들러 추가 | #3 |
| `domain-nearpick/.../entity/ReservationEntity.kt` | 수정 — product_id, status 인덱스 추가 | #4 |
| `domain-nearpick/.../entity/FlashPurchaseEntity.kt` | 수정 — product_id 인덱스 추가 | #4 |
| `domain/.../transaction/dto/TransactionDtos.kt` | 수정 — WishlistAddResponse 추가 | #5 |
| `app/.../controller/WishlistController.kt` | 수정 — WishlistAddResponse 사용 | #5 |
| `domain-nearpick/.../repository/WishlistRepository.kt` | 수정 — findTop200ByUser_Id 추가 | #6 |
| `app/.../controller/ProductController.kt` | 수정 — @Valid 순서 통일 | #7 |
| `app/.../controller/FlashPurchaseController.kt` | 수정 — @Valid 순서 통일 | #7 |
| `domain-nearpick/.../service/WishlistServiceImplTest.kt` | 신규 — 서비스 테스트 | #8 |
| `app/.../config/RateLimitFilterTest.kt` | 신규 — 필터 테스트 | #9 |

---

## 7. 구현 체크리스트

### Step 1 — P1 수정 (3개)
- [ ] `AdminController.withdrawUser()` → 200 응답으로 변경
- [ ] `WishlistServiceImpl.remove()` → `RESOURCE_NOT_FOUND` 사용
- [ ] `GlobalExceptionHandler` → 405, 400(MissingParam) 핸들러 추가

### Step 2 — P2 수정 (4개)
- [ ] `ReservationEntity` → `product_id`, `status` 인덱스 추가
- [ ] `FlashPurchaseEntity` → `product_id` 인덱스 추가
- [ ] `TransactionDtos.kt` → `WishlistAddResponse` 추가 + `WishlistController` 적용
- [ ] `WishlistRepository` → `findTop200ByUser_Id` 추가 + `WishlistServiceImpl` 적용

### Step 3 — P3 수정 (1개)
- [ ] `@Valid @RequestBody` 순서 통일 (`ProductController`, `FlashPurchaseController`)

### Step 4 — 테스트 추가 (2개)
- [ ] `WishlistServiceImplTest` 작성 (6개 케이스)
- [ ] `RateLimitFilterTest` 작성 (5개 케이스)

### Step 5 — 검증 (3개)
- [ ] `./gradlew build -x test` 빌드 성공
- [ ] `./gradlew test` 전체 테스트 통과
- [ ] Gap Analysis ≥ 90%

---

## 8. 아키텍처 검증 결과

| 검증 항목 | 결과 |
|---------|------|
| `app` → `domain-nearpick` 직접 import | ✅ 위반 없음 |
| 패키지 root 일관성 | ✅ 준수 |
| `@SpringBootApplication(scanBasePackages)` | ✅ `com.nearpick` 전체 스캔 |
| Pessimistic Lock (`findByIdWithLock`) | ✅ 정상 적용 |
| Batch Count Query | ✅ `countByProductIds` 정상 |
| 보안 헤더 | ✅ Phase 7에서 완료 |
| Rate Limit 경로 | ✅ Phase 7 버그 수정 완료 |

---

## 9. 변경 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0 | 2026-03-05 | 최초 작성 — 전체 코드 리뷰 후 9개 이슈 도출 | pdca-design |
