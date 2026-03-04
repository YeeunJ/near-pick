# [Plan] Phase 7 — Backend Security Hardening

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase7-security |
| Phase | Plan |
| 작성일 | 2026-03-04 |
| 참조 | `app/src/main/kotlin/com/nearpick/app/config/SecurityConfig.kt` |
| 참조 | `app/src/main/kotlin/com/nearpick/app/config/JwtTokenProvider.kt` |

---

## 배경 및 목적

Phase 4~4.5에서 Spring Security + JWT 기반의 API 인증/인가 구조가 완성됐다.
Phase 6(UI Integration)은 near-pick-web에서 진행 중이며, 백엔드와의 실제 HTTP 연동이 시작된다.

이 시점에서 다음 보안 공백이 존재한다:

1. **CORS 미설정** — 현재 CORS 설정 없음. near-pick-web(프론트엔드)에서 API 호출 시 브라우저 차단 발생
2. **Rate Limiting 없음** — 로그인 무차별 대입, API 남용 방어 수단 없음
3. **입력값 검증 미흡** — Controller 레이어에 Bean Validation 미적용
4. **JWT 보안 설정 미흡** — 만료 1시간(고정), Refresh Token 없음, secret 환경변수 미분리
5. **보안 헤더 없음** — X-Frame-Options, X-Content-Type-Options 등 미설정

### 왜 지금인가?

- near-pick-web Phase 6 API 연동 전 CORS 설정이 필수
- 배포(Phase 9) 전 보안 기본기 확보 필요
- SEO는 프론트엔드 소관이므로 이 Phase는 백엔드 보안만 담당

---

## 목표 (Goal)

1. **CORS 설정** — near-pick-web origin 허용, prod/local 환경 분리
2. **Rate Limiting** — 로그인 API 보호 (`/auth/login`), 일반 API 보호
3. **입력값 검증 강화** — Controller DTO에 `@Valid` + Bean Validation 적용
4. **JWT 보안 개선** — secret 환경변수화, 만료 정책 명확화
5. **보안 헤더 추가** — Spring Security 기본 보안 헤더 활성화

---

## 범위 (Scope)

### In Scope

| 항목 | 설명 |
|------|------|
| CORS 설정 | `CorsConfig.kt` 추가 — local/prod origin 분리 |
| Rate Limiting | Bucket4j 또는 Spring 자체 필터로 `/auth/login` throttle |
| Bean Validation | `@NotBlank`, `@Email`, `@Size` DTO 적용 + `@Valid` Controller |
| JWT secret 분리 | `application-local.properties`에서 관리, prod는 환경변수로 |
| 보안 헤더 | `SecurityConfig`에서 `headers()` 활성화 |

### Out of Scope

| 항목 | 이유 |
|------|------|
| HTTPS/TLS 설정 | Phase 9 배포 시 인프라 레벨에서 처리 |
| Refresh Token 구현 | 범위 확장 큼 — 별도 Phase로 분리 가능 |
| OAuth2 / 소셜 로그인 | 현재 요구사항 외 |
| SEO | near-pick-web 담당 |

---

## 기술 선택

### CORS

Spring MVC의 `CorsConfigurationSource` Bean 등록 방식 사용.
`@CrossOrigin` 어노테이션 방식은 Controller마다 관리 부담 → 전역 설정으로 통일.

```
환경별 허용 Origin:
- local: http://localhost:3000 (near-pick-web dev server)
- prod: https://nearpick.kr (예정 도메인) — 환경변수로 주입
```

### Rate Limiting

**Bucket4j** 라이브러리 채택 (Spring Boot starter 지원, in-memory 방식 간단):
- `/auth/login` : 분당 10회 제한 (IP 기반)
- 일반 API : 분당 100회 제한 (IP 기반)

> 대안: Spring Security의 `RequestRateLimiter` (Redis 필요) → 현재 단계는 Bucket4j로 충분

### Bean Validation

Spring Boot 내장 `spring-boot-starter-validation` 사용 (이미 의존성 추가 여부 확인 필요).

적용 대상 DTO:
- `AuthDtos.kt` — `LoginRequest`, `SignupRequest`
- `ProductDtos.kt` — `CreateProductRequest`
- `TransactionDtos.kt` — `CreateReservationRequest`

### JWT Secret

현재: `application-local.properties`에 하드코딩
개선: `application-local.properties`에서 플레이스홀더 유지, prod는 환경변수 `JWT_SECRET` 주입

---

## 구현 순서 (40단계)

### 1단계 — CORS 설정 (5단계)

1. `app/build.gradle.kts` 의존성 확인 (validation starter 없으면 추가)
2. `CorsConfig.kt` 생성 — `CorsConfigurationSource` Bean
3. `SecurityConfig.kt` 수정 — `.cors { it.configurationSource(corsConfigurationSource) }`
4. `application.properties`에 `cors.allowed-origins` 프로퍼티 추가
5. `application-local.properties`에 `cors.allowed-origins=http://localhost:3000` 설정

### 2단계 — 보안 헤더 (2단계)

6. `SecurityConfig.kt` 수정 — `headers()` 기본값 활성화 (X-Frame-Options DENY, X-Content-Type-Options nosniff 등)
7. CSRF: stateless API이므로 disable 유지 (현행 유지)

### 3단계 — Bean Validation 적용 (10단계)

8. `build.gradle.kts`에 `spring-boot-starter-validation` 추가 (없는 경우)
9. `AuthDtos.kt` — `LoginRequest`: `@Email @NotBlank email`, `@NotBlank @Size(min=8) password`
10. `AuthDtos.kt` — `SignupRequest`: email, password, role, shopName, businessRegNo 검증
11. `ProductDtos.kt` — `CreateProductRequest`: title, price(양수), type, address, coords 검증
12. `TransactionDtos.kt` — `CreateReservationRequest`: productId, quantity(양수), visitAt(미래) 검증
13. `AuthController.kt` — `@Valid` 추가
14. `ProductController.kt` — `@Valid` 추가
15. `ReservationController.kt` — `@Valid` 추가
16. `GlobalExceptionHandler.kt` — `MethodArgumentNotValidException` 처리 추가
17. 검증 실패 응답: `400 Bad Request` + `ApiResponse.error(errors)`

### 4단계 — Rate Limiting (8단계)

18. `app/build.gradle.kts`에 Bucket4j 의존성 추가
19. `RateLimitConfig.kt` 생성 — Bucket 설정 (login: 10/min, api: 100/min)
20. `RateLimitFilter.kt` 생성 — `OncePerRequestFilter` 구현
21. IP 추출: `X-Forwarded-For` → fallback to `remoteAddr`
22. `/auth/login` 경로 전용 throttle 로직
23. 초과 시 `429 Too Many Requests` 응답
24. `SecurityConfig.kt` — filter 순서 등록
25. `application.properties`에 rate limit 값 프로퍼티화 (`rate.limit.login`, `rate.limit.api`)

### 5단계 — JWT Secret 환경변수화 (5단계)

26. `application-local.properties` — `jwt.secret` 값 유지 (local 전용)
27. `application-prod.properties.example` — `jwt.secret=${JWT_SECRET}` 환경변수 참조로 변경
28. `application-prod.properties.example` — `jwt.expiration-ms=3600000` (1시간) 명시
29. `README.md` 또는 `docs/wiki/03-dev-guide.md` — 환경변수 설정 가이드 추가
30. `JwtTokenProvider.kt` — secret 길이 최소 32자 검증 (기존 `check()` 유지 확인)

### 6단계 — 테스트 및 검증 (10단계)

31. CORS preflight 테스트 (`OPTIONS /auth/login` → 200, Allow-Origin 헤더 확인)
32. 로그인 성공 후 `Authorization` 헤더 확인
33. Rate Limit 초과 시 429 응답 확인
34. 유효하지 않은 email 형식 → 400 응답 확인
35. password 8자 미만 → 400 응답 확인
36. 음수 price → 400 응답 확인
37. 보안 헤더 응답 확인 (`X-Frame-Options: DENY` 등)
38. 기존 25개 테스트 전체 통과 확인 (`./gradlew test`)
39. `./gradlew build -x test` 빌드 성공 확인
40. `./gradlew :app:bootRun` 구동 확인 (MySQL 필요)

---

## 성공 기준

| 기준 | 측정 방법 |
|------|---------|
| CORS: localhost:3000 → API 호출 성공 | 브라우저 or curl preflight 확인 |
| Rate Limit: 로그인 11회 → 429 | curl 반복 호출 |
| 유효하지 않은 입력 → 400 | 빈 email, 짧은 password |
| 보안 헤더 포함 | curl -I 응답 헤더 확인 |
| 기존 테스트 전체 통과 | `./gradlew test` |
| 빌드 성공 | `./gradlew build -x test` |
| Design-Implementation Gap | ≥ 90% |

---

## 위험 요소

| 위험 | 대응 |
|------|------|
| Bucket4j Spring Boot 4.x 호환성 | 사전 버전 확인 필요 |
| Bean Validation이 기존 테스트에 영향 | 테스트 request 객체에 유효한 값 사용 여부 확인 |
| CORS 설정 누락 시 프론트 API 호출 전면 차단 | 최우선 구현 |
| Rate Limit IP 추출 오류 (프록시 환경) | `X-Forwarded-For` 처리 필수 |

---

## 변경 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | pdca-plan |
