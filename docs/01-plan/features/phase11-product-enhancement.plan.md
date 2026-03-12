# Plan: phase11-product-enhancement

> Phase 11 — 상품 고도화 (이미지, 카테고리, 메뉴 옵션)

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase11-product-enhancement |
| **Phase** | 11 |
| **목표** | 상품 이미지 업로드, 카테고리 체계, 음식 카테고리 메뉴 옵션, 비음식 유연한 스펙 |
| **작성일** | 2026-03-12 |
| **브랜치 예정** | `feature/phase11-product-enhancement` |

---

## 배경 및 목적

현재 `ProductEntity`는 제목/설명/가격/재고 등 기본 정보만 보유한다.
실제 커머스 플랫폼으로 성장하려면 세 가지가 부족하다:

1. **이미지 없음**: 소비자가 상품 사진 없이 구매 결정을 내리기 어렵다
2. **카테고리 없음**: 음식/음료/뷰티 등 분류 없이 탐색 UX가 불완전하다
3. **옵션/스펙 없음**: 음식의 "사이즈·추가 토핑", 뷰티의 "용량·색상" 같은 상세 정보 표현 불가

Phase 11은 이 세 축을 백엔드에서 구현하여 Phase 6 프론트엔드 연동과 Phase 13 리뷰 시스템의 기반을 마련한다.

---

## 목표 기능

### 기능 1. 상품 카테고리

- `ProductCategory` enum: `FOOD` / `BEVERAGE` / `BEAUTY` / `DAILY` / `OTHER`
- `ProductEntity`에 `category` 컬럼 추가 (nullable, 기존 상품 하위 호환)
- 상품 등록/수정 API에 `category` 파라미터 추가
- `GET /products/nearby`, `GET /products` 에 `category` 필터 파라미터 지원

### 기능 2. 상품 이미지 업로드 (S3 Presigned URL)

업로드 플로우: 클라이언트 → 서버(Presigned URL 발급) → S3 직접 업로드 → 서버(URL 저장)

- `POST /products/{id}/images/presigned` — S3 Presigned PUT URL 발급
  - 최대 5장 제한 (초과 시 `PRODUCT_IMAGE_LIMIT_EXCEEDED`)
  - 허용 확장자: `jpg`, `jpeg`, `png`, `webp`
  - 파일 크기 제한: 최대 5MB (Presigned URL에 Content-Length 조건 포함)
- `POST /products/{id}/images` — 업로드 완료 후 URL 저장 (display_order 지정)
- `DELETE /products/{id}/images/{imageId}` — 이미지 삭제 (S3 오브젝트 + DB)
- `PUT /products/{id}/images/order` — 이미지 순서 변경
- 신규 엔티티 `ProductImageEntity`: `(id, product_id, s3_key, url, display_order, created_at)`
- Local 환경: `PRODUCT_IMAGE_UPLOAD_ENABLED=false` 시 mock URL 반환 (S3 없이 개발 가능)

### 기능 3. 음식 카테고리 — 메뉴 옵션 시스템

`category = FOOD | BEVERAGE`인 상품에만 적용.

- `ProductMenuOptionGroup`: 옵션 그룹 (예: "사이즈", "추가 옵션")
  - `required: Boolean`, `maxSelect: Int` (다중 선택 허용 여부)
- `ProductMenuChoice`: 그룹 내 선택지 (예: "Small +0원", "Large +500원")
  - `name: String`, `additionalPrice: Int`
- API:
  - `POST /products/{id}/menu-options` — 옵션 그룹 + 선택지 일괄 등록
  - `PUT /products/{id}/menu-options/{groupId}` — 그룹 수정
  - `DELETE /products/{id}/menu-options/{groupId}` — 그룹 삭제
  - `GET /products/{id}` 응답에 `menuOptions` 포함 (category=FOOD/BEVERAGE 시)

### 기능 4. 비음식 카테고리 — 유연한 스펙 속성

`category = BEAUTY | DAILY | OTHER`인 상품에 적용.

- `ProductEntity`에 `specs TEXT` 컬럼 추가 (JSON 직렬화, nullable)
- 구조 예시:
  ```json
  [{"key": "용량", "value": "200ml"}, {"key": "색상", "value": "피치"}]
  ```
- `ProductCreateRequest` / `ProductUpdateRequest`에 `specs: List<ProductSpec>?` 추가
- `ProductDetailResponse`에 `specs` 포함

---

## 범위 (In / Out of Scope)

### In Scope

| 항목 | 이유 |
|------|------|
| `ProductCategory` enum + `category` 필드 | 핵심 분류 체계, 모든 후속 필터에 필요 |
| S3 Presigned URL 이미지 업로드 플로우 | 직접 업로드로 서버 부하 최소화 |
| `ProductImageEntity` DB 저장 | 이미지 순서·관리 필요 |
| 음식 메뉴 옵션 (그룹 + 선택지) | 음식 상품 UX 핵심 |
| 비음식 스펙 (JSON TEXT) | 유연한 속성 표현, 복잡도 낮음 |
| `category` 필터 (nearby, 목록) | 탐색 UX 개선 |
| Flyway V5 마이그레이션 | 스키마 변경 관리 |
| 단위 테스트 | 신규 서비스 로직 검증 |

### Out of Scope

| 항목 | 이유 |
|------|------|
| CloudFront CDN 설정 | Phase 15 배포 시 인프라와 함께 구성 |
| 이미지 리사이징 / 썸네일 생성 | Lambda@Edge 등 별도 인프라 필요 |
| 소상공인 UI (상품 등록 폼 개선) | near-pick-web Phase 6 담당 |
| 카테고리별 인기도 랭킹 | Phase 13+ 후순위 |
| 옵션 재고 분리 관리 | 복잡도 과도, 후순위 |

---

## 기술 방향

### 신규 엔티티

**ProductImageEntity** (`product_images`)
```
id              BIGINT PK AUTO_INCREMENT
product_id      BIGINT FK → products(id) ON DELETE CASCADE
s3_key          VARCHAR(500) NOT NULL       -- S3 오브젝트 키 (삭제 시 사용)
url             VARCHAR(1000) NOT NULL      -- 접근 URL (CloudFront 또는 S3 직접)
display_order   INT NOT NULL DEFAULT 0
created_at      DATETIME(6) NOT NULL
```

**ProductMenuOptionGroupEntity** (`product_menu_option_groups`)
```
id              BIGINT PK AUTO_INCREMENT
product_id      BIGINT FK → products(id) ON DELETE CASCADE
name            VARCHAR(50) NOT NULL        -- "사이즈", "추가 옵션"
required        BOOLEAN NOT NULL DEFAULT FALSE
max_select      INT NOT NULL DEFAULT 1
display_order   INT NOT NULL DEFAULT 0
```

**ProductMenuChoiceEntity** (`product_menu_choices`)
```
id              BIGINT PK AUTO_INCREMENT
group_id        BIGINT FK → product_menu_option_groups(id) ON DELETE CASCADE
name            VARCHAR(50) NOT NULL        -- "Small", "Large"
additional_price INT NOT NULL DEFAULT 0
display_order   INT NOT NULL DEFAULT 0
```

### ProductEntity 변경

```kotlin
// 추가 컬럼
@Enumerated(EnumType.STRING)
@Column(length = 20)
var category: ProductCategory? = null

@Column(columnDefinition = "TEXT")
var specs: String? = null  // JSON 직렬화 (ObjectMapper 사용)
```

### S3 연동 방식

- `S3Client` (AWS SDK v2) 사용
- `S3Config.kt` (`app` 모듈, `@Profile("!test")`)
- Presigned URL 유효 시간: 5분
- S3 키 패턴: `products/{productId}/images/{uuid}.{ext}`
- 환경변수: `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- Local 개발: `PRODUCT_IMAGE_UPLOAD_ENABLED=false` → mock URL 반환 (`/images/mock/{filename}`)
- Test: `PRODUCT_IMAGE_UPLOAD_ENABLED=false` 또는 MockBean 처리

### 카테고리 필터

```
GET /products/nearby?category=FOOD
GET /products?category=BEAUTY
```
- 쿼리 파라미터 optional (미전달 시 전체)
- `ProductRepository` JPQL/Native 쿼리에 조건 추가

---

## 예상 파일 변경 목록

### 신규 파일

| 모듈 | 파일 |
|------|------|
| `domain` | `product/ProductCategory.kt` (enum) |
| `domain` | `product/dto/ProductImageDtos.kt` |
| `domain` | `product/dto/ProductMenuOptionDtos.kt` |
| `domain` | `product/ProductImageService.kt` (interface) |
| `domain` | `product/ProductMenuOptionService.kt` (interface) |
| `domain-nearpick` | `product/entity/ProductImageEntity.kt` |
| `domain-nearpick` | `product/entity/ProductMenuOptionGroupEntity.kt` |
| `domain-nearpick` | `product/entity/ProductMenuChoiceEntity.kt` |
| `domain-nearpick` | `product/repository/ProductImageRepository.kt` |
| `domain-nearpick` | `product/repository/ProductMenuOptionGroupRepository.kt` |
| `domain-nearpick` | `product/repository/ProductMenuChoiceRepository.kt` |
| `domain-nearpick` | `product/service/ProductImageServiceImpl.kt` |
| `domain-nearpick` | `product/service/ProductMenuOptionServiceImpl.kt` |
| `domain-nearpick` | `product/client/S3Client.kt` (또는 config) |
| `app` | `controller/ProductImageController.kt` |
| `app` | `controller/ProductMenuOptionController.kt` |
| `app` | `config/S3Config.kt` |
| `app/resources/db/migration` | `V5__product_enhancement.sql` |

### 변경 파일

| 모듈 | 파일 | 변경 내용 |
|------|------|-----------|
| `domain` | `product/dto/ProductDtos.kt` | `category`, `specs`, `images`, `menuOptions` 추가 |
| `domain-nearpick` | `product/entity/ProductEntity.kt` | `category`, `specs` 컬럼 추가 |
| `domain-nearpick` | `product/service/ProductServiceImpl.kt` | 카테고리 필터, specs 처리 |
| `domain-nearpick` | `product/repository/ProductRepository.kt` | 카테고리 필터 쿼리 |
| `domain-nearpick` | `product/mapper/ProductMapper.kt` | images, menuOptions, specs 매핑 |

---

## 성공 기준

| 항목 | 목표 |
|------|------|
| 카테고리 등록·필터 | 상품 생성 시 category 지정, nearby/목록 필터 정상 동작 |
| 이미지 업로드 플로우 | Presigned URL 발급 → URL 저장 → 조회·삭제 정상 |
| 이미지 5장 제한 | 6번째 추가 시 `PRODUCT_IMAGE_LIMIT_EXCEEDED` 400 반환 |
| 메뉴 옵션 CRUD | FOOD 상품에 옵션 그룹·선택지 등록·수정·삭제 |
| 스펙 속성 저장 | BEAUTY 상품에 specs JSON 저장·조회 |
| 빌드·테스트 통과 | `./gradlew build` 성공, 신규 테스트 10개 이상 |
| Gap Analysis | Match Rate ≥ 90% |

---

## 의존성 확인

- [x] Phase 4 API (`ProductEntity`, `ProductService`) — 완료
- [x] Phase 7 Security (MERCHANT role 인가) — 완료
- [x] Phase 9 Redis 캐싱 (`products-detail` 캐시 — 이미지 추가 시 무효화 처리 필요) — 완료
- [ ] AWS S3 버킷 설정 필요 (Local: mock 모드로 우회 가능)
- [ ] AWS 자격증명 환경변수 설정 (Local: `application-local.properties`)

---

## 다음 단계

```
/pdca design phase11-product-enhancement
```
