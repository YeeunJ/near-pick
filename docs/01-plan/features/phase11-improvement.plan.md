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
- local/test 환경에서 Kakao API 키 없이도 앱이 실행되어야 하나, 실행 시 API 키 미설정으로 오류 가능
- `LocationSearchServiceImpl`이 `KakaoLocationClient` 구체 클래스에 직접 의존

**구현 목표**
- `LocationClient` 인터페이스 생성 (`domain/location/`)
- `KakaoLocationClient` → `@Profile("!local & !test")` 로 격리
- `StubLocationClient` (`app/`, `@Profile("local")`) — 테스트 주소 데이터 반환
- `NoOpLocationClient` (`app/src/test/`, `@Profile("test")`) — 빈 리스트 반환
- `LocationSearchServiceImpl`이 `LocationClient` 인터페이스 주입

---

### 개선 항목 4: Strategy Pattern — FlashPurchaseEventProducer

**문제**
- `FlashPurchaseProducer`가 `@Component` 단일 구현체로 직접 주입됨
- local/test 환경에서 Kafka 없이도 앱이 실행되어야 하나, send() 호출 시 Kafka 연결 필요
- `FlashPurchaseConsumer`, `FlashPurchaseDlqConsumer`의 `@KafkaListener`가 local/test에서 불필요하게 등록됨

**구현 목표**
- `FlashPurchaseEventProducer` 인터페이스 생성 (`domain-nearpick/messaging/`)
- `FlashPurchaseProducer` → `KafkaFlashPurchaseProducer` 로 리네임, `@Profile("!local & !test")`
- `LocalFlashPurchaseEventProducer` (`domain-nearpick/main/`, `@Profile("local")`) — 로그만 기록
- `NoOpFlashPurchaseEventProducer` (`domain-nearpick/main/`, `@Profile("test")`) — 아무것도 하지 않음
- `FlashPurchaseConsumer`, `FlashPurchaseDlqConsumer` → `@Profile("!local & !test")`
- `FlashPurchaseServiceImpl`이 `FlashPurchaseEventProducer` 인터페이스 주입

---

## Out of Scope
- 테스트 보완 (이미 설계 예상치 이상으로 구현 완료)
- 기타 Phase 11 외 기능

## Implementation Order
1. **Cache Invalidation** — 서비스 레이어 어노테이션 추가 (낮은 위험도)
2. **thumbnailUrl** — 네이티브 쿼리 수정 + Projection + Mapper (중간 위험도, 쿼리 변경)
3. **LocationClient Strategy** — 인터페이스 추출 + 프로필 분리 (중간 위험도)
4. **FlashPurchaseEventProducer Strategy** — 인터페이스 추출 + 프로필 분리 (중간 위험도)

## Success Criteria
- `products-detail` 캐시가 이미지/메뉴 변경 즉시 evict 됨
- `GET /api/products/nearby` 응답의 `thumbnailUrl`에 첫 번째 이미지 URL 포함
- local 프로필: Kakao API 키 없이 위치 검색 → stub 데이터 반환
- local 프로필: Kafka 없이 선착순 구매 → 로그 기록 후 PENDING 반환
- test 프로필: Kakao/Kafka 실제 연결 없이 모든 컨트롤러 테스트 통과
- 기존 테스트 전부 GREEN, 빌드 통과

## Estimated Effort
- Cache Invalidation: 30분 (어노테이션 추가)
- thumbnailUrl: 1시간 (쿼리 수정 + Projection + Mapper)
- LocationClient Strategy: 30분 (인터페이스 + 3개 구현체)
- FlashPurchaseEventProducer Strategy: 45분 (인터페이스 + 4개 구현체 + Consumer 프로필 추가)
