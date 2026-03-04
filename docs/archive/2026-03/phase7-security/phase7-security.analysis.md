# Phase 7 Security Hardening Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: near-pick (backend)
> **Analyst**: gap-detector
> **Date**: 2026-03-05
> **Design Doc**: [phase7-security.design.md](../02-design/features/phase7-security.design.md)

---

## 1. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| CORS 설정 | 100% | ✅ |
| 보안 헤더 | 95% | ✅ |
| Rate Limiting — Config | 90% | ✅ |
| Rate Limiting — Filter (경로 매칭) | 70% | ❌ |
| 의존성 | 100% | ✅ |
| Properties 설정 | 100% | ✅ |
| Filter 등록 순서 | 100% | ✅ |
| **종합** | **94%** | **✅** |

> ⚠️ Rate Limiting 경로 버그로 인해 로그인/회원가입 throttle이 실제로 동작하지 않음 → 수정 필요

---

## 2. Gap Analysis (Design vs Implementation)

### 2.1 CORS 설정

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `CorsConfig.kt` 신규 생성 | `app/.../config/CorsConfig.kt` ✅ | ✅ |
| `allowedOrigins` = `cors.allowed-origins` 프로퍼티 | `@Value("\${cors.allowed-origins}")` + split ✅ | ✅ |
| `allowedMethods` = GET/POST/PUT/PATCH/DELETE/OPTIONS | 정확히 일치 ✅ | ✅ |
| `allowedHeaders` = `["*"]` | `listOf("*")` ✅ | ✅ |
| `allowCredentials` = true | `config.allowCredentials = true` ✅ | ✅ |
| `maxAge` = 3600 | `config.maxAge = 3600L` ✅ | ✅ |
| `/**` 경로에 등록 | `source.registerCorsConfiguration("/**", config)` ✅ | ✅ |
| `SecurityConfig` — `.cors()` 추가 | `.cors { it.configurationSource(corsConfigurationSource) }` ✅ | ✅ |

**CORS Score: 100%**

### 2.2 보안 헤더

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `frameOptions { it.deny() }` | ✅ 구현 | ✅ |
| `contentTypeOptions { }` | ✅ 구현 | ✅ |
| `httpStrictTransportSecurity { includeSubDomains, maxAge=31536000 }` | ✅ 구현 | ✅ |
| `xssProtection { }` | ❌ 미구현 | ⚠️ 의도적 제거 |

**변경 이유**: Spring Security 7.0.3에서 `xssProtection()` DSL이 `IllegalArgumentException`을 발생시킴 → 제거. 현대 브라우저에서 `X-XSS-Protection` 헤더는 deprecated 상태이므로 보안 영향 없음.

**보안 헤더 Score: 95%** (의도적 변경, 보안 영향 없음)

### 2.3 Rate Limiting — 의존성 및 Config

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `bucket4j-core:8.10.1` 의존성 | `app/build.gradle.kts` ✅ | ✅ |
| `RateLimitConfig.kt` 신규 생성 | ✅ 생성 | ✅ |
| `loginBandwidth` = 10/min | `capacity(10).refillGreedy(10, 1분)` ✅ | ✅ |
| `apiBandwidth` = 200/min | `capacity(200).refillGreedy(200, 1분)` ✅ | ✅ |
| `signupBandwidth` = 5/min (별도 설정) | ❌ 미구현 — login/signup 동일 bucket 사용 | ⚠️ |

**Config Score: 90%** (signup 별도 제한 미분리 — 영향도 낮음)

### 2.4 Rate Limiting — Filter (경로 매칭) ❌

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `RateLimitFilter.kt` 신규 생성 | ✅ 생성 | ✅ |
| IP 기반 throttle | `resolveIp(request)` + `ConcurrentHashMap` ✅ | ✅ |
| `X-Forwarded-For` 처리 | ✅ 구현 | ✅ |
| 429 응답 | `HttpStatus.TOO_MANY_REQUESTS` + JSON ✅ | ✅ |
| auth 경로 감지: `POST /api/auth/login` | `request.requestURI == "/auth/login"` ❌ — `/api/` prefix 누락 | ❌ |
| signup 경로 감지: `POST /api/auth/signup/**` | `request.requestURI.startsWith("/auth/signup")` ❌ — `/api/` prefix 누락 | ❌ |

**버그 원인**: `AuthController`의 실제 매핑은 `/api/auth`이나, `RateLimitFilter`는 `/auth/login`, `/auth/signup`으로 체크. `SecurityConfig`도 `/api/auth/**`로 업데이트됐으나 `RateLimitFilter`가 누락됨.

**실제 영향**: login/signup API 호출 시 항상 `apiBuckets`(200/min)로 분류됨 → login throttle 미적용

**Filter Score: 70%** (경로 버그 → 기능 미동작)

### 2.5 Properties 설정

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `application.properties` — `cors.allowed-origins` 추가 | `cors.allowed-origins=http://localhost:3000` ✅ | ✅ |
| `application-prod.properties.example` — `cors.allowed-origins=${CORS_ALLOWED_ORIGINS}` | ✅ 추가 | ✅ |

**Properties Score: 100%**

### 2.6 Filter 등록 순서

| Design 항목 | 구현 | 상태 |
|------------|------|------|
| `rateLimitFilter` → `jwtAuthenticationFilter` 순서로 실행 | `addFilterBefore(jwtAuthFilter, UPAF)` → `addFilterBefore(rateLimitFilter, JwtFilter)` ✅ | ✅ |

> Design 문서에서 `addFilterBefore(rateLimitFilter, JwtFilter)` 먼저 등록 제안 → Spring Security 7 호환성을 위해 순서 변경. 실행 순서는 동일.

**Filter 등록 Score: 100%**

---

## 3. 발견된 버그

### Bug #1 — RateLimitFilter 경로 불일치 (Critical)

**파일**: `app/src/main/kotlin/com/nearpick/app/config/RateLimitFilter.kt:30`

```kotlin
// 현재 (잘못됨)
val isAuthPath = request.method == "POST" &&
    (request.requestURI == "/auth/login" || request.requestURI.startsWith("/auth/signup"))

// 수정 필요
val isAuthPath = request.method == "POST" &&
    (request.requestURI == "/api/auth/login" || request.requestURI.startsWith("/api/auth/signup"))
```

**원인**: `AuthController`는 `@RequestMapping("/api/auth")`로 매핑되어 있으나 Filter의 경로 문자열은 `/auth/`로 하드코딩됨. `SecurityConfig`의 `/auth/**` → `/api/auth/**` 변경이 `RateLimitFilter`에 반영되지 않음.

**영향**: login/signup 요청이 `loginBuckets`(10/min) 대신 `apiBuckets`(200/min)로 처리 → 로그인 brute force 방어 미작동

---

## 4. 수정 필요 항목

| 항목 | 우선순위 | 파일 |
|------|---------|------|
| RateLimitFilter 경로 수정 (`/auth/` → `/api/auth/`) | 필수 | `RateLimitFilter.kt` |
| signup 별도 5/min bucket | 선택 (낮음) | `RateLimitConfig.kt`, `RateLimitFilter.kt` |

---

## 5. 종합 점수

```
+------------------------------------------+
|  Overall Score: 94/100                    |
+------------------------------------------+
|  CORS 설정:           100%  ✅            |
|  보안 헤더:            95%  ✅ (xss 제거) |
|  Rate Limit Config:   90%  ✅             |
|  Rate Limit Filter:   70%  ❌ (경로 버그) |
|  의존성:              100%  ✅            |
|  Properties:          100%  ✅            |
|  Filter 등록 순서:    100%  ✅            |
+------------------------------------------+
|  종합 점수:  94%  ✅ (90% 기준 충족)       |
+------------------------------------------+
```

**Match Rate: 94%** — 90% 기준 충족. 단, Rate Limiting 경로 버그 수정 필수.

---

## 6. Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-05 | Initial gap analysis | gap-detector |
