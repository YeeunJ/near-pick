# 도메인 용어사전

NearPick 프로젝트에서 사용하는 핵심 도메인 개념 정의.
코드 네이밍과 문서 작성 시 반드시 이 정의를 따른다.

---

## 사용자 도메인

### User
- **정의**: 플랫폼에 가입한 모든 사용자의 공통 계정 정보
- **영문**: User
- **관련 Entity**: `UserEntity` (`domain-nearpick`)
- **관련 Enum**: `UserRole`, `UserStatus`
- **비즈니스 규칙**:
  - 역할(role)에 따라 프로필이 분리됨 (소비자·소상공인·관리자)
  - 이메일 기반 인증, 중복 불가

### 소비자 (Consumer)
- **정의**: 상품을 탐색하고 구매하는 일반 사용자
- **영문**: Consumer
- **관련 Entity**: `ConsumerProfileEntity`
- **비즈니스 규칙**: `UserRole.CONSUMER`인 사용자만 소비자 프로필 보유

### 소상공인 (Merchant)
- **정의**: 상품과 할인권을 등록·판매하는 사업자
- **영문**: Merchant
- **관련 Entity**: `MerchantProfileEntity`
- **비즈니스 규칙**:
  - 사업자 등록번호(`businessRegNo`) 필수
  - `UserRole.MERCHANT`인 사용자만 상품 등록 가능

### 관리자 (Admin)
- **정의**: 플랫폼을 운영·모니터링하는 내부 사용자
- **영문**: Admin
- **관련 Entity**: `AdminProfileEntity`
- **관련 Enum**: `AdminLevel` (SUPER, MANAGER, VIEWER)

---

## 상품 도메인

### 상품 (Product)
- **정의**: 소상공인이 판매하는 실물 상품 또는 서비스
- **영문**: Product
- **관련 Entity**: `ProductEntity`
- **관련 Enum**: `ProductType`, `ProductStatus`
- **비즈니스 규칙**:
  - `GENERAL`: 일반 상품 (재고 있는 한 지속 판매)
  - `FLASH_SALE`: 선착순 상품 (수량·시간 제한)
  - 가격은 원(KRW) 단위 정수

### 상품 상태 (ProductStatus)
| 상태 | 설명 |
|------|------|
| `ACTIVE` | 판매 중 |
| `SOLD_OUT` | 재고 소진 (일시적) |
| `CLOSED` | 소상공인이 판매 종료 |
| `FORCE_CLOSED` | 관리자가 강제 종료 |

### 인기점수 (PopularityScore)
- **정의**: 상품의 인기를 수치화한 점수 (찜+예약+구매를 가중 합산)
- **영문**: PopularityScore
- **관련 Entity**: `PopularityScoreEntity`
- **비즈니스 규칙**: 배치(batch) 작업으로 주기적 갱신, 상품과 1:1 관계

---

## 거래 도메인

### 찜 (Wishlist)
- **정의**: 소비자가 관심 있는 상품을 저장하는 기능
- **영문**: Wishlist
- **관련 Entity**: `WishlistEntity`
- **비즈니스 규칙**: 소비자-상품 조합 유니크 (`uq_wishlist_user_product`)

### 예약 (Reservation)
- **정의**: 소비자가 방문 수령 일시를 사전에 예약하는 거래
- **영문**: Reservation
- **관련 Entity**: `ReservationEntity`
- **관련 Enum**: `ReservationStatus`
- **비즈니스 규칙**:
  - 소비자가 방문 일시 선택 → 소상공인이 확인/거절
  - 배송 없음, 직접 방문 수령

### 예약 상태 (ReservationStatus)
| 상태 | 설명 |
|------|------|
| `PENDING` | 예약 요청 대기 중 |
| `CONFIRMED` | 소상공인 확인 완료 |
| `CANCELLED` | 취소됨 |
| `COMPLETED` | 방문 수령 완료 |

### 선착순 구매 (FlashPurchase)
- **정의**: 한정 수량 상품을 선착순으로 구매하는 거래
- **영문**: FlashPurchase
- **관련 Entity**: `FlashPurchaseEntity`
- **관련 Enum**: `FlashPurchaseStatus`
- **비즈니스 규칙**:
  - `ProductType.FLASH_SALE` 상품에만 적용
  - 수량 소진 시 상품 상태 → `SOLD_OUT` 자동 전환

### 선착순 구매 상태 (FlashPurchaseStatus)
| 상태 | 설명 |
|------|------|
| `PURCHASED` | 구매 완료 |
| `CANCELLED` | 취소됨 |
| `REFUNDED` | 환불됨 |

---

## 위치 관련

### 위치 좌표
- **정의**: 상품(가게) 위치를 나타내는 위도/경도 쌍
- **저장 방식**: `DECIMAL(10, 7)` — 소수점 7자리 (약 1cm 정밀도)
- **필드명**: `shopLat` (위도), `shopLng` (경도)
- **향후**: PostGIS `POINT` 타입으로 마이그레이션 예정
