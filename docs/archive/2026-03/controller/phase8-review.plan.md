# [Plan] Phase 8 — Code Review & Quality

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase8-review |
| Phase | Plan |
| 작성일 | 2026-03-05 |
| 참조 | 전체 소스 코드 (app, domain, domain-nearpick, common) |
| 선행 Phase | Phase 7 (Security Hardening) ✅ |

---

## 배경 및 목적

Phase 1~7을 거쳐 백엔드 핵심 기능이 완성됐다.
Phase 9(배포) 전 마지막 품질 점검으로, **전체 코드베이스를 체계적으로 리뷰**하여 기술 부채를 해소한다.

현재 상태:
- 총 25개 컨트롤러 테스트 + 4개 서비스 테스트 + 4개 도메인 모델 테스트 = **52개(도메인) + 25개(앱) = 77개 테스트** 통과 중
- 멀티 모듈 구조 (`app` → `domain` → `common`, `domain-nearpick` runtimeOnly)
- Pessimistic Lock (FlashPurchase), Batch Count Query (getMyProducts) 등 성능 고려 이미 적용

---

## 목표 (Goal)

1. **아키텍처 준수 검증** — 모듈 간 의존성 규칙(runtimeOnly 경계) 확인
2. **API 응답 일관성** — 에러 코드, 상태 코드, 응답 형식 통일 여부
3. **누락 테스트 보완** — 서비스 레이어 커버리지 공백 식별 및 추가
4. **N+1 쿼리 / 성능 리스크** — 잠재적 쿼리 폭발 지점 확인
5. **코드 컨벤션 준수** — CONVENTIONS.md 기준 전체 파일 점검
6. **보안 재검토** — Phase 7 이후 잔여 보안 이슈

---

## 범위 (Scope)

### In Scope

| 영역 | 검토 항목 |
|------|---------|
| 아키텍처 | 모듈 의존성, 패키지 구조, `runtimeOnly` 경계 침범 여부 |
| API 설계 | HTTP 상태 코드, 에러 응답 형식, 경로 네이밍 일관성 |
| 비즈니스 로직 | 재고 처리 동시성, 권한 검증, 상태 전환 규칙 |
| 테스트 | 서비스 테스트 누락 파악, 경계값/예외 케이스 보완 |
| 성능 | N+1 가능성, 인덱스 누락, 불필요한 전체 조회 |
| 코드 품질 | Kotlin idiom 활용, null 안전성, 중복 코드 |
| 보안 | 인가 로직 검토, 민감 데이터 로깅 여부 |
| 문서 | Swagger 어노테이션 완결성 |

### Out of Scope

| 항목 | 이유 |
|------|------|
| 신규 기능 개발 | 리뷰 Phase이므로 기존 코드 개선에 집중 |
| DB 마이그레이션 | Phase 9 배포 시 처리 |
| 인프라 설정 | Phase 9 담당 |
| Refresh Token / OAuth | 별도 Phase |

---

## 검토 대상 파일 목록

### app 모듈 (Controller, Config)

| 파일 | 검토 포인트 |
|------|-----------|
| `AuthController.kt` | `@Valid`, 응답 타입 일관성, 경로 `/api/auth/**` |
| `ProductController.kt` | 권한 검증, 페이지네이션 파라미터 기본값 |
| `ReservationController.kt` | 상태 전환 경로, 권한별 endpoint 분리 |
| `FlashPurchaseController.kt` | 선착순 구매 동시성 처리 확인 |
| `WishlistController.kt` | 중복 찜 에러 처리 |
| `MerchantController.kt` | 소상공인 정보 수정 범위 |
| `AdminController.kt` | 관리자 전용 권한 확인 |
| `GlobalExceptionHandler.kt` | 미처리 예외 케이스 |
| `SecurityConfig.kt` | 권한 매핑 완결성 |
| `RateLimitFilter.kt` | path 수정 이후 정상 동작 확인 |

### domain 모듈 (Service interface, DTO)

| 파일 | 검토 포인트 |
|------|-----------|
| `AuthDtos.kt` | Bean Validation 어노테이션 완결성 |
| `ProductDtos.kt` | 필드 타입 적절성, nullable 여부 |
| `TransactionDtos.kt` | 요청/응답 DTO 분리 |
| `model/` (Email, Password, Location 등) | 도메인 모델 불변성, 유효성 로직 |

### domain-nearpick 모듈 (ServiceImpl, Entity, Repository)

| 파일 | 검토 포인트 |
|------|-----------|
| `AuthServiceImpl.kt` | 비밀번호 해싱, 중복 가입 처리 |
| `ProductServiceImpl.kt` | N+1 쿼리 (wishlist count), Batch 처리 확인 |
| `FlashPurchaseServiceImpl.kt` | Pessimistic Lock 유효성 |
| `ReservationServiceImpl.kt` | 상태 전환 규칙 |
| Entity (`UserEntity` 등) | `@Column` 제약 조건, 인덱스 |
| Repository | `@Query` JPQL 정확성, Native Query 위험성 |

### common 모듈

| 파일 | 검토 포인트 |
|------|-----------|
| `ErrorCode.kt` | HTTP 상태 코드 정확성, 메시지 국제화 여부 |
| `ApiResponse.kt` | 응답 래퍼 형식 일관성 |

---

## 구현 순서 (30단계)

### 1단계 — 아키텍처 & 의존성 검증 (5단계)

1. `app` 모듈이 `domain-nearpick` 클래스를 직접 import하는지 확인 (runtimeOnly 경계 위반)
2. 모듈별 패키지 root 일관성 확인 (`com.nearpick.app`, `com.nearpick.domain`, `com.nearpick.nearpick`, `com.nearpick.common`)
3. 순환 의존성 확인 (`./gradlew :app:dependencies` 출력 검토)
4. `@SpringBootApplication(scanBasePackages=["com.nearpick"])` 설정 확인
5. `JpaConfig.kt` — `@EnableJpaRepositories` 스캔 범위 확인

### 2단계 — API 일관성 검토 (5단계)

6. 모든 Controller endpoint의 응답 타입 `ApiResponse<T>` 래핑 여부 확인
7. HTTP 상태 코드 정확성 — 생성: 201, 조회: 200, 삭제: 200/204
8. `ErrorCode` vs 실제 발생 예외 매핑 누락 여부 확인
9. Swagger `@Operation`, `@Parameter` 어노테이션 누락 endpoint 파악
10. 경로 네이밍 일관성 — `/api/{resource}/{id}/{action}` 패턴 점검

### 3단계 — 비즈니스 로직 & 보안 검토 (8단계)

11. `FlashPurchaseServiceImpl` — `findByIdWithLock` Pessimistic Lock 정합성 확인
12. `ReservationServiceImpl` — 상태 전환 규칙 (`PENDING → CONFIRMED/CANCELLED`) 누락 케이스
13. `ProductServiceImpl.close()` — 소유권 검증 (`merchantId == product.merchant.userId`) 로직 완결성
14. `WishlistServiceImpl` — 중복 찜 `ALREADY_WISHLISTED` 처리 확인
15. `AuthServiceImpl` — 비밀번호 검증 전 계정 정지 상태 확인 순서
16. `AdminController` — `ROLE_ADMIN` 이외 접근 차단 검증
17. 민감 정보 (`password`, `jwt.secret`) 로그 출력 여부 확인
18. `JwtTokenProvider` — 만료 토큰 처리 예외 핸들링

### 4단계 — 성능 & 쿼리 검토 (5단계)

19. `ProductServiceImpl.getDetail()` — wishlist/reservation/purchase count 3번 별도 쿼리 → Batch 가능성 검토
20. `getMyProducts()` — `countByProductIds` Batch 쿼리 정상 동작 확인
21. Entity `@Index` 누락 컬럼 파악 (`userId`, `productId`, `status` 등)
22. `findNearby` Native Query — Haversine 공식 정확성 및 인덱스 활용 여부
23. Lazy Loading 트랩 — Serialization 시 `LazyInitializationException` 가능성

### 5단계 — 테스트 보완 (5단계)

24. 서비스 레이어 테스트 현황 파악 — `AuthServiceImpl`, `ProductServiceImpl`, `WishlistServiceImpl` 테스트 존재 여부
25. `FlashPurchaseServiceImplTest` — 동시 구매 시나리오 테스트 존재 여부
26. Controller 테스트 — 403 Forbidden, 404 Not Found 케이스 커버 여부
27. `RateLimitFilter` 단위 테스트 추가 (429 응답 검증)
28. 경계값 테스트 — stock=0일 때 OUT_OF_STOCK 처리

### 6단계 — 코드 품질 & 컨벤션 (2단계)

29. CONVENTIONS.md 기준으로 파일별 네이밍, 포맷 최종 점검
30. `./gradlew build` 최종 통과 확인

---

## 성공 기준

| 기준 | 측정 방법 |
|------|---------|
| 아키텍처 경계 위반 0건 | `./gradlew :app:dependencies` 확인 |
| 모든 테스트 통과 | `./gradlew test` |
| API 응답 형식 일관성 | Controller 전체 응답 타입 확인 |
| 발견된 버그 전수 수정 | 리뷰 결과 이슈 리스트 해소 |
| Design-Implementation Gap ≥ 90% | `/pdca analyze phase8-review` |

---

## 예상 발견 항목 (사전 분석)

아키텍처 리뷰 중 발견 예상 이슈:

| 항목 | 심각도 | 근거 |
|------|:-----:|------|
| `ProductServiceImpl.getDetail()` — count 쿼리 3번 | 낮음 | 트래픽 낮을 때 무시 가능, 개선 권장 |
| 서비스 테스트 누락 (`WishlistServiceImpl` 등) | 중간 | 현재 테스트 목록에 없음 |
| Entity `@Index` 미선언 | 중간 | 운영 데이터 증가 시 풀스캔 위험 |
| Swagger 어노테이션 불완전 | 낮음 | API 문서화 품질 |

---

## 위험 요소

| 위험 | 대응 |
|------|------|
| 리뷰 범위 확장 | 신규 기능 개발은 Phase 9 이후로 엄격히 제한 |
| 테스트 추가 시 기존 테스트 영향 | PR 단위로 소분화하여 검증 |
| 성능 개선이 기능 변경을 수반할 경우 | 별도 Phase로 분리 |

---

## 변경 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0 | 2026-03-05 | 최초 작성 | pdca-plan |
