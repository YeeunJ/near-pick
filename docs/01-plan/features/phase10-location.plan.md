# Plan: phase10-location

> Phase 10 — 위치 & 지도 서비스

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase10-location |
| **Phase** | 10 |
| **목표** | 소비자 위치 관리 + 주소 검색 + 저장 위치 CRUD |
| **작성일** | 2026-03-11 |
| **브랜치 예정** | `feature/phase10-location` |

---

## 배경 및 목적

NearPick의 핵심 기능은 **"근처 인기 상품 탐색"**이다.
현재 `GET /products/nearby`는 클라이언트가 직접 lat/lng를 파라미터로 전달하지만,
실제 사용 시나리오에서는:

1. **현재 위치 갱신**: 앱 실행 시 GPS 위치를 서버에 저장 → `nearby` 조회에 자동 반영
2. **저장 위치 관리**: 집/직장 등 자주 쓰는 위치 최대 5개 저장 → 간편 선택
3. **주소 검색**: 카카오 주소 API로 텍스트 → 좌표 변환 (소비자·소상공인 모두 사용)

이 세 기능이 없으면 UX가 불완전하고, 프론트엔드 Phase 6 연동도 어렵다.

---

## 목표 기능

### 기능 1. 현재 위치 갱신 (Consumer)

- `PATCH /consumers/me/location` — 현재 위치 (lat, lng) 저장
- `ConsumerProfileEntity.currentLat / currentLng` 기존 필드 활용
- 인증 필요 (CONSUMER role)
- 갱신 시 `updatedAt` 자동 반영

### 기능 2. 저장 위치 CRUD (Consumer)

- `SavedLocation` 신규 엔티티 필요 (consumer_id, label, lat, lng, is_default)
- 최대 5개 제한 (초과 시 `SAVED_LOCATION_LIMIT_EXCEEDED` 에러)
- API:
  - `GET /consumers/me/locations` — 목록 조회
  - `POST /consumers/me/locations` — 저장 위치 추가
  - `PUT /consumers/me/locations/{id}` — 수정 (label, is_default)
  - `DELETE /consumers/me/locations/{id}` — 삭제
  - `PATCH /consumers/me/locations/{id}/default` — 기본 위치 지정

### 기능 3. 주소 검색 API 연동 (카카오)

- `GET /location/search?query=서울시 강남구` — 텍스트 → 좌표 변환
- 카카오 로컬 API (주소 검색) 연동: `https://dapi.kakao.com/v2/local/search/address.json`
- 인증 필요 (CONSUMER 또는 MERCHANT)
- 결과: `{ address: string, lat: double, lng: double }[]` (최대 5건)
- API Key: 환경변수 `KAKAO_REST_API_KEY`

### 기능 4. nearby 쿼리 위치 소스 선택 지원 (옵션)

- `GET /products/nearby?locationSource=current` → ConsumerProfile.currentLat/Lng 사용
- `GET /products/nearby?locationSource=saved&savedLocationId=1` → 저장 위치 사용
- `GET /products/nearby?lat=37.5&lng=127.0` → 기존 직접 전달 방식 유지 (하위 호환)
- 기본값: `locationSource=direct` (lat/lng 직접 전달, 기존 동작)

---

## 범위 (In / Out of Scope)

### In Scope

| 항목 | 이유 |
|------|------|
| 현재 위치 갱신 API | ConsumerProfile 기존 필드 활용, 구현 비용 낮음 |
| 저장 위치 CRUD | 핵심 UX, Phase 6 연동에 필수 |
| 카카오 주소 검색 연동 | 소상공인 shop_lat/lng 등록에도 재사용 가능 |
| nearby locationSource 파라미터 | 저장 위치 선택 UX 완성 |
| Flyway 마이그레이션 | `saved_locations` 테이블 신규 |
| 단위 테스트 | LocationService, SavedLocationService |

### Out of Scope

| 항목 | 이유 |
|------|------|
| 지도 UI (핀, 클러스터링) | near-pick-web Phase 6 담당 |
| 실시간 위치 추적 (WebSocket) | 복잡도 과도, 후순위 |
| 카카오 지도 SDK 연동 | 프론트엔드 담당 |
| 위치 이력 저장 | 현재 요구사항 외 |

---

## 기술 방향

### 신규 엔티티: SavedLocation

```
saved_locations
  id           BIGINT PK AUTO_INCREMENT
  consumer_id  BIGINT FK → consumer_profiles(user_id)
  label        VARCHAR(50)   -- "집", "직장" 등
  lat          DECIMAL(10,7) NOT NULL
  lng          DECIMAL(10,7) NOT NULL
  is_default   BOOLEAN DEFAULT FALSE
  created_at   DATETIME(6) NOT NULL
```

제약:
- `consumer_id`당 최대 5개 (서비스 레이어 검증)
- `is_default=true`는 1개만 허용 → 다른 항목 자동 false 처리

### 카카오 API 연동 방식

- `WebClient` (Spring WebFlux) 또는 `RestClient` (Spring 6.1+) 사용
- `KakaoLocationClient` 인프라 빈 (`domain-nearpick` 또는 `app`)
- API Key 환경변수: `KAKAO_REST_API_KEY` (local: `application-local.properties`, prod: 환경변수)
- 타임아웃: 3초, 실패 시 `EXTERNAL_API_UNAVAILABLE` 예외

### nearby locationSource

- `ProductNearbyRequest` DTO에 `locationSource: LocationSource` enum 추가
  - `DIRECT` (기본), `CURRENT`, `SAVED`
- `ProductService.getNearby()` 내부에서 소스에 따라 lat/lng 결정
- CONSUMER role 아닐 시 `CURRENT`/`SAVED` 사용 불가 → `DIRECT` 강제

---

## 예상 파일 변경 목록

| 모듈 | 파일 | 변경 유형 |
|------|------|-----------|
| `domain` | `LocationSource.kt` (enum) | 신규 |
| `domain` | `SavedLocationService.kt` (interface) | 신규 |
| `domain` | `LocationSearchService.kt` (interface) | 신규 |
| `domain` | `dto/SavedLocationDto.kt` | 신규 |
| `domain` | `dto/LocationSearchResult.kt` | 신규 |
| `domain` | `dto/ProductNearbyRequest.kt` | 수정 (locationSource 추가) |
| `domain-nearpick` | `SavedLocationEntity.kt` | 신규 |
| `domain-nearpick` | `SavedLocationRepository.kt` | 신규 |
| `domain-nearpick` | `SavedLocationServiceImpl.kt` | 신규 |
| `domain-nearpick` | `KakaoLocationClient.kt` | 신규 |
| `domain-nearpick` | `LocationSearchServiceImpl.kt` | 신규 |
| `domain-nearpick` | `ProductServiceImpl.kt` | 수정 (locationSource 처리) |
| `app` | `ConsumerController.kt` (또는 LocationController.kt) | 신규/수정 |
| `app/resources` | `V4__add_saved_locations.sql` | 신규 |
| `app/resources` | `application-local.properties` | 수정 (KAKAO_REST_API_KEY) |

---

## 성공 기준

| 항목 | 목표 |
|------|------|
| API 응답 정상 | 현재 위치 갱신, 저장 위치 CRUD, 주소 검색 모두 동작 |
| 최대 5개 제한 | 6번째 추가 시 400 에러 반환 |
| 기본 위치 단일성 | is_default=true는 항상 1개 |
| nearby 위치 소스 | `locationSource=current` 로 nearby 정상 조회 |
| 단위 테스트 | 신규 테스트 8개 이상 |
| Gap Analysis | Match Rate ≥ 90% |

---

## 의존성 확인

- [x] Phase 4 API (ConsumerProfile, ProductNearby) — 완료
- [x] Phase 7 Security (JWT CONSUMER role) — 완료
- [x] Phase 9 Redis 캐싱 (nearby 캐시 무효화 고려) — 완료
- [ ] 카카오 REST API Key 발급 필요 (외부)

---

## 다음 단계

```
/pdca design phase10-location
```
