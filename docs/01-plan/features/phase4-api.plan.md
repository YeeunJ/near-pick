# [Plan] Phase 4 — API Design & Implementation

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase4-api |
| Phase | Plan |
| 작성일 | 2026-02-24 |
| 프로젝트 | NearPick |
| 레벨 | Enterprise |
| 참조 | Phase 3 목업 (`docs/02-design/features/phase3-mockup.design.md`) |

---

## 1. 목표 (Objective)

Phase 3에서 도출한 20개 엔드포인트를 Spring Boot 멀티모듈 구조에 맞게 설계·구현한다.
JWT 기반 인증을 포함하여 실제 동작하는 REST API를 완성한다.

---

## 2. 배경 (Background)

- Phase 1 엔티티(9개)가 `domain-nearpick`에 구현돼 있음
- Phase 2 컨벤션에 따라 Service 인터페이스 → `domain`, 구현체 → `domain-nearpick`, Controller → `app`
- Phase 3 목업에서 필요 API 20개 확정

**엔티티 → 실제 필드 확인 결과 수정 사항**:
- `ProductStatus`: DRAFT, ACTIVE, PAUSED, CLOSED, FORCE_CLOSED (SOLD_OUT 없음)
- `FlashPurchaseStatus`: PENDING, CONFIRMED, CANCELLED, COMPLETED
- `ProductEntity`: `stock` 필드로 재고 관리, `availableFrom/Until`으로 판매 기간 관리

---

## 3. 범위 (Scope)

### In Scope (Phase 4 구현 대상)

| 도메인 | API 수 | 우선순위 |
|--------|:-----:|:-------:|
| 인증 (Auth) | 3 | 최우선 |
| 상품 (Product) | 5 | 높음 |
| 찜 (Wishlist) | 3 | 높음 |
| 예약 (Reservation) | 5 | 높음 |
| 선착순 구매 (FlashPurchase) | 2 | 높음 |
| 소상공인 대시보드 | 1 | 중간 |
| 관리자 (Admin) | 5 | 중간 |

**총 24개** (Phase 3 20개 + 추가 확정 필요 API)

### Out of Scope
- 결제 연동 (후순위)
- 알림(Push/Email) (후순위)
- 이미지 업로드 (후순위)
- PopularityScore 배치 갱신 (후순위)

---

## 4. 기술 결정 (Technical Decisions)

| 항목 | 결정 | 이유 |
|------|------|------|
| 인증 방식 | JWT (stateless) | 확장성, REST 원칙 |
| JWT 라이브러리 | `jjwt 0.12.6` | Spring Boot 4 호환, 현재 표준 |
| Security 프레임워크 | Spring Security 6 | Spring Boot 4 기본 |
| 위치 기반 검색 | Haversine 공식 (Native Query) | H2·PostgreSQL 모두 호환 |
| 비밀번호 해시 | `BCryptPasswordEncoder` | Spring Security 내장 |
| 입력 유효성 검사 | `spring-boot-starter-validation` (`@Valid`) | BOM 포함, 추가 의존성 없음 |
| 페이지네이션 | Spring Data `Pageable` | 표준, 일관성 |

### JWT 배치 전략

```
app/
├── config/SecurityConfig.kt          ← FilterChain, BCrypt Bean
├── config/JwtAuthenticationFilter.kt ← 요청마다 토큰 검증
└── config/JwtTokenProvider.kt        ← 토큰 생성·검증 (app 전용)

domain/ auth/
└── AuthService.kt                    ← 회원가입/로그인 계약

domain-nearpick/ auth/
└── AuthServiceImpl.kt                ← 비즈니스 로직, UserRepository 호출
```

---

## 5. 구현 순서

```
1. 의존성 추가 (build.gradle.kts)
2. common: ErrorCode 보강
3. Auth — 회원가입 / 로그인 / JWT
4. Security Config — JWT 필터, 경로 권한
5. Product — 목록(위치 기반), 상세, 등록, 종료
6. Wishlist — 추가, 제거, 내 목록
7. Reservation — 생성, 내 목록, 확정/거절/취소
8. FlashPurchase — 구매, 내 목록
9. Dashboard — 소상공인 요약
10. Admin — 사용자·상품 관리
```

---

## 6. 작업 목록

- [ ] 의존성 추가 (security, validation, jjwt)
- [ ] `common/ErrorCode` 보강
- [ ] Auth 도메인 (Service 인터페이스 + DTO + 구현체 + Controller)
- [ ] JWT 설정 (SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter)
- [ ] Product 도메인
- [ ] Wishlist 도메인
- [ ] Reservation 도메인
- [ ] FlashPurchase 도메인
- [ ] Merchant Dashboard
- [ ] Admin 도메인
- [ ] `./gradlew build` 통과

---

## 7. 완료 기준 (Definition of Done)

- [ ] 모든 API 엔드포인트 구현 완료
- [ ] JWT 인증·인가 동작 확인
- [ ] `./gradlew build` 성공
- [ ] H2 Console에서 데이터 생성·조회 확인

---

## 8. 다음 단계

완료 후 → `/pdca design phase4-api`
