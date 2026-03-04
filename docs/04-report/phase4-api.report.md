# [Report] Phase 4 — API Design & Implementation

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4-api |
| Branch | `feature/phase4-rework` |
| PR | #7 (https://github.com/YeeunJ/near-pick/pull/7) |
| 작성일 | 2026-02-26 |
| **최종 Match Rate** | **96%** |
| PDCA 이터레이션 | 1회 |
| 빌드 상태 | ✅ BUILD SUCCESSFUL |

---

## PDCA 사이클 요약

```
[Plan] → [Design] → [Do] ✅ → [Check] → [Act-1] ✅ → [Report] ✅
                              72%        96%
```

| Phase | 결과 |
|-------|------|
| 설계 기준 | `docs/02-design/features/phase3-mockup.design.md` (Phase 3에서 도출) |
| 구현 | `feature/phase4-rework` — 62개 파일, 2,199줄 추가 |
| Gap Analysis | 초기 72% → 이터레이션 1회 후 96% |
| 완료 기준 | 24/24 설계 API 구현, 빌드 통과 |

---

## 구현 범위

### 1. 인프라 (Infrastructure)

| 항목 | 내용 |
|------|------|
| Local DB | MySQL 8.4 (Docker Compose) |
| 설정 분리 | `application.properties` (공통) / `application-local.properties` (gitignored) / `application-prod.properties` (gitignored) |
| 보안 | `.gitignore` — 시크릿 파일 제외 |

### 2. 도메인 Value Objects (`domain/model/`)

| VO | 검증 규칙 |
|----|----------|
| `Email` | 형식 검증, `masked()` / `localPart()` 헬퍼 |
| `BusinessRegNo` | 한국 사업자번호 `XXX-XX-XXXXX` 형식 |
| `Location` | lat: -90~90, lng: -180~180 범위 |
| `Password` | 최소 8자, 숫자 1개 이상, 문자 1개 이상 |

### 3. 인증 & 보안

| 항목 | 구현 내용 |
|------|----------|
| JWT | jjwt 0.12.6, HS256, `@PostConstruct` fail-fast 키 검증 |
| Spring Security | Stateless, Role-based, `@EnableMethodSecurity` |
| 버그 수정 | 정지 계정 로그인 허용 → 상태 체크 추가 |
| 버그 수정 | 이메일 중복 race condition → `DataIntegrityViolationException` catch |

### 4. Mapper 패턴

모든 Entity↔DTO 변환을 Kotlin `object` mapper로 중앙화:
- `UserMapper`, `ProductMapper`, `TransactionMapper`, `MerchantMapper`, `AdminMapper`

### 5. 구현된 API (24개)

#### 인증 (3)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/signup/consumer` | 소비자 회원가입 |
| POST | `/api/auth/signup/merchant` | 소상공인 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) |

#### 상품 (5)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/products/nearby` | 위치 기반 인기 상품 (Haversine) |
| GET | `/api/products/{productId}` | 상품 상세 |
| POST | `/api/products` | 상품 등록 (소상공인) |
| PATCH | `/api/products/{productId}/close` | 상품 종료 |
| GET | `/api/products/me` | 내 상품 목록 |

#### 찜 (3)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/wishlists` | 찜 추가 |
| DELETE | `/api/wishlists/{productId}` | 찜 제거 |
| GET | `/api/wishlists/me` | 내 찜 목록 |

#### 예약 (5)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/reservations` | 예약 생성 |
| GET | `/api/reservations/me` | 내 예약 내역 |
| PATCH | `/api/reservations/{id}/cancel` | 예약 취소 |
| GET | `/api/reservations/merchant` | 대기 예약 목록 (소상공인) |
| PATCH | `/api/reservations/{id}/confirm` | 예약 확정 |

#### 선착순 구매 (2)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/flash-purchases` | 선착순 구매 |
| GET | `/api/flash-purchases/me` | 내 구매 내역 |

#### 소상공인 (2+1)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/merchants/me/dashboard` | 대시보드 요약 |
| GET | `/api/merchants/me/profile` | 프로필 조회 (추가) |

#### 관리자 (5+1)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/admin/users` | 사용자 목록 (필터/검색) |
| PATCH | `/api/admin/users/{id}/suspend` | 계정 정지 |
| DELETE | `/api/admin/users/{id}` | 강제 탈퇴 |
| GET | `/api/admin/products` | 상품 목록 (필터) |
| PATCH | `/api/admin/products/{id}/force-close` | 상품 강제 종료 |
| GET | `/api/admin/profile` | 관리자 프로필 (추가) |

---

## 버그 수정 & 설계 개선

| 항목 | 이전 | 수정 후 |
|------|------|---------|
| 로그인 상태 체크 | 없음 (정지 계정 로그인 가능) | `SUSPENDED` → 403, `WITHDRAWN` → 401 |
| 이메일 중복 race condition | `exists() + save()` 패턴 | `DataIntegrityViolationException` catch |
| JPQL enum 하드코딩 | `com.nearpick.domain.ReservationStatus.PENDING` | `@Param` 방식 |
| getMyProducts N+1 | 상품 수만큼 wishlistCount 쿼리 | 단일 `GROUP BY` 배치 쿼리 |
| 대시보드 상품 OOM | 무제한 `findAll` | `findTop100` 제한 |
| FlashPurchase 동시성 | 단순 재고 확인 후 차감 | `@Lock(PESSIMISTIC_WRITE)` 비관적 락 |
| 사업자번호 형식 미검증 | `@NotBlank`만 | `BusinessRegNo` VO로 형식 강제 |
| 비밀번호 강도 미검증 | `@Size(min=8)`만 | `Password` VO (숫자+문자 필수) |
| SecurityConfig 경로 불일치 | `/products/...` (prefix 없음) | `/api/products/...` (실제 경로 일치) |

---

## 아키텍처 결정 사항 (ADR)

### ADR-1: JWT 생성 위치
**결정**: `AuthController`에서 JWT 생성 (`JwtTokenProvider.createToken`)
**이유**: `domain-nearpick`이 `runtimeOnly` 의존성이므로 JWT 라이브러리를 `domain-nearpick`에 추가하면 컴파일 경계가 무너짐. `app` 레이어에서 생성이 아키텍처에 맞음.

### ADR-2: productId 위치 (Wishlist/Reservation/FlashPurchase)
**결정**: Request body에 `productId` 포함
**이유**: Phase 3 설계 스펙 준수 (`POST /wishlists { productId }`)

### ADR-3: 상품 등록 위치
**결정**: 상품 위치(`shopLat/Lng`)는 `MerchantProfile`에서 자동 복사
**이유**: 소상공인 가게 위치와 상품 위치가 항상 동일한 비즈니스 룰 반영

### ADR-4: Value Object 범위
**결정**: 회원가입 시에만 VO 생성 (로그인/조회 시 미사용)
**이유**: DB에서 이미 검증된 데이터에 VO 재생성은 불필요한 비용

---

## Gap Analysis 결과

| 단계 | Match Rate | 주요 원인 |
|------|-----------|----------|
| 초기 구현 후 | 72% | SecurityConfig 경로 불일치 (Critical), me/my 불일치, PATCH→POST |
| Iteration 1 후 | **96%** | 전체 Gap 수정 완료 |

잔여 4%: SecurityConfig `{id}` vs `{reservationId}` path variable 명칭 차이 (기능 동작 무관)

---

## 파일 구조 (주요 변경 파일)

```
├── docker-compose.yml                     # MySQL 8.4 로컬 개발
├── app/
│   ├── build.gradle.kts                   # JWT, Security 의존성
│   ├── src/main/kotlin/com/nearpick/app/
│   │   ├── config/
│   │   │   ├── JwtTokenProvider.kt
│   │   │   ├── JwtAuthenticationFilter.kt
│   │   │   ├── SecurityConfig.kt
│   │   │   └── GlobalExceptionHandler.kt
│   │   └── controller/
│   │       ├── AuthController.kt
│   │       ├── ProductController.kt
│   │       ├── WishlistController.kt
│   │       ├── ReservationController.kt
│   │       ├── FlashPurchaseController.kt
│   │       ├── MerchantController.kt
│   │       └── AdminController.kt
│   └── src/main/resources/
│       ├── application.properties          # 공통 (커밋)
│       ├── application-local.properties.example
│       └── application-prod.properties.example
├── domain/
│   └── src/main/kotlin/com/nearpick/domain/
│       ├── model/                          # Value Objects
│       │   ├── Email.kt, BusinessRegNo.kt, Location.kt, Password.kt
│       ├── auth/                           # AuthService, DTOs
│       ├── product/                        # ProductService, DTOs
│       ├── transaction/                    # Wishlist/Reservation/FlashPurchase
│       ├── merchant/                       # MerchantService, DTOs
│       └── admin/                         # AdminService, DTOs
└── domain-nearpick/
    └── src/main/kotlin/com/nearpick/nearpick/
        ├── auth/AuthServiceImpl.kt
        ├── product/                        # Entity, Repo, ServiceImpl, Mapper
        ├── transaction/                    # Entity, Repo, ServiceImpl, Mapper
        └── user/                          # Entity, Repo, ServiceImpl, Mapper
```

---

## 다음 Phase 제안

| Phase | 내용 | 준비도 |
|-------|------|--------|
| Phase 5 | Design System (UI 컴포넌트) | 백엔드 완료 후 프론트 시작 가능 |
| Phase 7 | SEO / Security 강화 | CORS, Rate Limiting, HTTPS 설정 |
| 선택 | Swagger/OpenAPI 문서화 | 현재 API 주석 없음 — 추가 권장 |
| 선택 | 인덱스 최적화 | MySQL 쿼리 실행 계획 검토 필요 |

---

*Generated by PDCA Report — phase4-api | 2026-02-26*
