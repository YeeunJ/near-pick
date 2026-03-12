# Plan: phase11-improvement

## Feature Overview
Phase 11 Gap Analysis(96%)에서 도출된 미구현 항목 2건 + 환경 분리 전략(Strategy Pattern) 3건을 보완한다.
설계-구현 불일치를 해소하고 환경별 의존성을 명확히 분리한다.

## Background
- **출처**: `docs/03-analysis/phase11-product-enhancement.analysis.md` — Areas for Improvement
- **현재 Match Rate**: 96% (67.5/70)
- **목표 Match Rate**: 100% + Strategy Pattern 환경 분리

## Scope

### 개선 항목 1: Cache Invalidation (@CacheEvict)

**문제**
- 설계: 이미지/메뉴 변경 시 `products-detail` 캐시 evict 명시
- 구현: `ProductImageServiceImpl`, `ProductMenuOptionServiceImpl`에 `@CacheEvict` 없음
- 영향: TTL 60초 내에 변경 사항이 캐시에 반영되지 않아 오래된 데이터 노출 가능

**구현 목표**
- `ProductImageServiceImpl.saveImageUrl`, `deleteImage`, `reorderImages` → `@CacheEvict(products-detail)`
- `ProductMenuOptionServiceImpl.saveMenuOptions`, `deleteMenuOptionGroup` → `@CacheEvict(products-detail)`

---

### 개선 항목 2: thumbnailUrl (근처 상품 조회)

**문제**
- 설계: `ProductSummaryResponse.thumbnailUrl = images[0].url`
- 구현: `findNearby` 네이티브 쿼리에서 첫 번째 이미지 조회 없음 → 항상 `null`
- 영향: 프론트엔드에서 근처 상품 리스트에 썸네일 표시 불가

**구현 목표**
- `findNearby` 쿼리에 `product_images` LEFT JOIN 추가 (`display_order = 0` 기준 첫 번째 이미지)
- `ProductNearbyProjection`에 `thumbnailUrl: String?` 추가
- `ProductMapper.toSummaryResponse()`에 `thumbnailUrl` 매핑

---

### 개선 항목 3: Strategy Pattern — LocationClient

**문제**
- `KakaoLocationClient`가 `@Component` 단일 구현체로 직접 주입됨
- test 환경에서 Kakao API 실제 연결 없이도 서비스 테스트 가능해야 함
- `LocationSearchServiceImpl`이 `KakaoLocationClient` 구체 클래스에 직접 의존

**프로필 전략**
- `local`: Kakao API 실제 연결 사용 (stub 불필요)
- `test`: 실제 연결 없이 빈 리스트 반환 (NoOp)
- `그 외 (prod 등)`: Kakao API 실제 연결 사용

**구현 목표**
- `LocationClient` 인터페이스 생성 (`domain/location/`)
- `KakaoLocationClient` → `@Profile("!test")` 로 격리
- `NoOpLocationClient` (`app/src/test/`, `@Profile("test")`) — 빈 리스트 반환
- `LocationSearchServiceImpl`이 `LocationClient` 인터페이스 주입

---

### 개선 항목 4: Strategy Pattern — FlashPurchaseEventProducer

**문제**
- `FlashPurchaseProducer`가 `@Component` 단일 구현체로 직접 주입됨
- test 환경에서 Kafka 실제 연결 없이도 컨트롤러 테스트 가능해야 함
- `FlashPurchaseConsumer`, `FlashPurchaseDlqConsumer`의 `@KafkaListener`가 test에서 불필요하게 등록됨

**프로필 전략**
- `local`: Kafka 실제 연결 사용 (stub 불필요)
- `test`: 실제 연결 없이 no-op 처리
- `그 외 (prod 등)`: Kafka 실제 연결 사용

**구현 목표**
- `FlashPurchaseEventProducer` 인터페이스 생성 (`domain-nearpick/messaging/`)
- `KafkaFlashPurchaseProducer` (`@Profile("!test")`) — Kafka 실제 전송
- `NoOpFlashPurchaseEventProducer` (`domain-nearpick/main/`, `@Profile("test")`) — 아무것도 하지 않음
- `FlashPurchaseConsumer`, `FlashPurchaseDlqConsumer` → `@Profile("!test")`
- `FlashPurchaseServiceImpl`이 `FlashPurchaseEventProducer` 인터페이스 주입

---

### 개선 항목 5: Strategy Pattern — ImageStorageService

**문제**
- `S3ImageStorageService`가 `@Profile("!local & !test")`로 이미 분리되어 있음
- `LocalImageStorageService`가 `@Profile("local")`만 커버 → test에서는 no-op만 동작

**프로필 전략**
- `local`: 로컬 파일 시스템 저장 (LocalImageStorageService)
- `test`: 로컬 파일 시스템 저장 (LocalImageStorageService — test에서도 실제 파일 경로 반환)
- `그 외 (prod 등)`: S3 저장 (S3ImageStorageService)

**구현 목표**
- `LocalImageStorageService` → `@Profile("local | test")` 로 확장
- `NoOpImageStorageService` 삭제 (LocalImageStorageService가 test도 커버)

---

## Out of Scope
- 테스트 보완 (이미 설계 예상치 이상으로 구현 완료)
- 기타 Phase 11 외 기능

## Implementation Order
1. **Cache Invalidation** — 서비스 레이어 어노테이션 추가 (낮은 위험도)
2. **thumbnailUrl** — 네이티브 쿼리 수정 + Projection + Mapper (중간 위험도, 쿼리 변경)
3. **LocationClient Strategy** — 인터페이스 추출 + `@Profile("!test")` 분리
4. **FlashPurchaseEventProducer Strategy** — 인터페이스 추출 + `@Profile("!test")` 분리
5. **ImageStorageService Strategy** — `LocalImageStorageService`를 `local | test`로 확장

## Success Criteria
- `products-detail` 캐시가 이미지/메뉴 변경 즉시 evict 됨
- `GET /api/products/nearby` 응답의 `thumbnailUrl`에 첫 번째 이미지 URL 포함
- local 프로필: Kakao API, Kafka 실제 연결, 로컬 스토리지 사용
- test 프로필: Kakao/Kafka 실제 연결 없이 모든 컨트롤러 테스트 통과, 로컬 스토리지 사용
- prod 프로필: Kakao API, Kafka, S3 실제 연결 사용
- 기존 테스트 전부 GREEN, 빌드 통과

## Profile Matrix

| 기능 | local | test | prod |
|------|-------|------|------|
| ImageStorage | LocalImageStorageService | LocalImageStorageService | S3ImageStorageService |
| LocationClient | KakaoLocationClient | NoOpLocationClient | KakaoLocationClient |
| FlashPurchaseProducer | KafkaFlashPurchaseProducer | NoOpFlashPurchaseEventProducer | KafkaFlashPurchaseProducer |
| FlashPurchaseConsumer | 활성 | 비활성 | 활성 |

## Estimated Effort
- Cache Invalidation: 30분 (어노테이션 추가)
- thumbnailUrl: 1시간 (쿼리 수정 + Projection + Mapper)
- LocationClient Strategy: 30분 (인터페이스 + 2개 구현체)
- FlashPurchaseEventProducer Strategy: 30분 (인터페이스 + 2개 구현체 + Consumer 프로필 추가)
- ImageStorageService Strategy: 15분 (프로필 수정)
