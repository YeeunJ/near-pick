# [Design] Phase 7 — Backend Security Hardening

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase7-security |
| Phase | Design |
| 작성일 | 2026-03-04 |
| 참조 Plan | `docs/01-plan/features/phase7-security.plan.md` |

---

## 1. 현황 분석 (As-Is)

코드 분석 결과, 계획 단계보다 **이미 구현된 항목**이 많다.

### 1.1 이미 완료된 항목 (변경 불필요)

| 항목 | 파일 | 상태 |
|------|------|------|
| Bean Validation 의존성 | `app/build.gradle.kts` — `spring-boot-starter-validation` | ✅ 완료 |
| DTO 유효성 어노테이션 | `AuthDtos`, `ProductDtos`, `TransactionDtos` — `@NotBlank`, `@Email`, `@Size`, `@Positive` 등 | ✅ 완료 |
| Controller `@Valid` 적용 | `AuthController`, `ProductController`, `ReservationController`, `WishlistController`, `FlashPurchaseController` | ✅ 완료 |
| 검증 실패 예외 처리 | `GlobalExceptionHandler` — `MethodArgumentNotValidException` → 400 | ✅ 완료 |
| JWT secret 환경변수 | `application-prod.properties.example` — `jwt.secret=${JWT_SECRET}` | ✅ 완료 |
| JWT 만료 설정 | `jwt.expiration-ms=3600000` (1시간) | ✅ 완료 |

> **참고:** MerchantController, AdminController는 `@RequestBody` 없이 `@RequestParam`/`@PathVariable`만 사용 → `@Valid` 적용 대상 아님.

### 1.2 실제 구현 대상 (To-Be)

| 항목 | 현황 | 작업 내용 |
|------|------|---------|
| CORS | 미설정 — 브라우저에서 near-pick-web API 호출 시 차단 | `CorsConfig.kt` 신규 생성 |
| 보안 헤더 | `SecurityConfig`에 `headers()` 미설정 | `SecurityConfig.kt` 수정 |
| Rate Limiting | 미구현 | `RateLimitFilter.kt` 신규 생성 + Bucket4j 의존성 |

---

## 2. CORS 설계

### 2.1 구성 방식

Spring MVC의 `CorsConfigurationSource` Bean 전역 등록. Controller별 `@CrossOrigin` 사용 금지.

### 2.2 파일 명세

**`app/src/main/kotlin/com/nearpick/app/config/CorsConfig.kt`** (신규)

```kotlin
@Configuration
class CorsConfig {

    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: String

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
```

### 2.3 SecurityConfig 수정

```kotlin
// 기존 http 설정에 추가
.cors { it.configurationSource(corsConfigurationSource) }
```

`CorsConfig.kt`의 Bean을 생성자 주입으로 받아 연결.

### 2.4 프로퍼티 설정

| 파일 | 키 | 값 |
|------|----|----|
| `application.properties` | `cors.allowed-origins` | (placeholder — 환경별 override 필수) |
| `application-local.properties` | `cors.allowed-origins` | `http://localhost:3000` |
| `application-prod.properties.example` | `cors.allowed-origins` | `${CORS_ALLOWED_ORIGINS}` |

### 2.5 허용 정책

| 항목 | 값 | 이유 |
|------|----|----|
| Methods | GET, POST, PUT, PATCH, DELETE, OPTIONS | RESTful API 전체 |
| Headers | `*` | Authorization 포함 허용 |
| Credentials | `true` | JWT Bearer 토큰 전송 지원 |
| MaxAge | 3600초 | preflight 캐시 1시간 |

---

## 3. 보안 헤더 설계

### 3.1 SecurityConfig 수정

Spring Security 기본 보안 헤더를 명시적으로 활성화.

```kotlin
.headers { headers ->
    headers
        .frameOptions { it.deny() }                    // X-Frame-Options: DENY (clickjacking 방어)
        .contentTypeOptions { }                        // X-Content-Type-Options: nosniff
        .httpStrictTransportSecurity { hsts ->         // HSTS (HTTPS 강제)
            hsts.includeSubDomains(true).maxAgeInSeconds(31536000)
        }
        .xssProtection { }                             // X-XSS-Protection: 1; mode=block
}
```

### 3.2 적용 헤더 목록

| 헤더 | 값 | 목적 |
|------|----|----|
| `X-Frame-Options` | `DENY` | 클릭재킹(iframe 삽입) 방어 |
| `X-Content-Type-Options` | `nosniff` | MIME 타입 스니핑 방어 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 강제 (prod) |
| `X-XSS-Protection` | `1; mode=block` | 구형 브라우저 XSS 필터 |

> `Cache-Control`, `Pragma` 등 Spring Security 기본 헤더는 자동 포함됨.

---

## 4. Rate Limiting 설계

### 4.1 라이브러리 선택: Bucket4j

| 항목 | 선택 | 이유 |
|------|------|------|
| 라이브러리 | `com.bucket4j:bucket4j-core:8.10.1` | Spring Boot 4.x 호환, 인메모리, 의존성 최소 |
| 저장소 | In-Memory (JVM) | Redis 없이 단일 서버 환경 충분 |
| 방식 | Token Bucket 알고리즘 | 버스트 허용, 부드러운 제한 |

> 대안 고려: Spring Cloud Gateway RateLimiter (Redis 필요) → 현재 단계 오버스펙

### 4.2 Rate Limit 정책

| 경로 | 제한 | 기준 | 초과 응답 |
|------|------|------|---------|
| `POST /api/auth/login` | 분당 10회 | IP 기반 | `429 Too Many Requests` |
| `POST /api/auth/signup/**` | 분당 5회 | IP 기반 | `429 Too Many Requests` |
| 그 외 전체 API | 분당 200회 | IP 기반 | `429 Too Many Requests` |

### 4.3 파일 명세

**`app/src/main/kotlin/com/nearpick/app/config/RateLimitConfig.kt`** (신규)

```kotlin
@Configuration
class RateLimitConfig {

    @Bean
    fun loginBucket(): Bandwidth =
        Bandwidth.builder()
            .capacity(10)
            .refillGreedy(10, Duration.ofMinutes(1))
            .build()

    @Bean
    fun apiBucket(): Bandwidth =
        Bandwidth.builder()
            .capacity(200)
            .refillGreedy(200, Duration.ofMinutes(1))
            .build()
}
```

**`app/src/main/kotlin/com/nearpick/app/config/RateLimitFilter.kt`** (신규)

```kotlin
@Component
class RateLimitFilter(
    private val loginBandwidth: Bandwidth,
    private val apiBandwidth: Bandwidth,
) : OncePerRequestFilter() {

    // IP → Bucket 맵 (캐시)
    private val loginBuckets = ConcurrentHashMap<String, Bucket>()
    private val apiBuckets   = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val ip = resolveIp(request)
        val isLoginPath = request.method == "POST" &&
            (request.requestURI == "/api/auth/login" || request.requestURI.startsWith("/api/auth/signup"))

        val bucket = if (isLoginPath) {
            loginBuckets.computeIfAbsent(ip) { Bucket.builder().addLimit(loginBandwidth).build() }
        } else {
            apiBuckets.computeIfAbsent(ip) { Bucket.builder().addLimit(apiBandwidth).build() }
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"success":false,"message":"Too many requests. Please try again later."}""")
        }
    }

    private fun resolveIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
}
```

### 4.4 Filter 등록

`SecurityConfig`에서 `RateLimitFilter`를 `JwtAuthenticationFilter` 앞에 등록:

```kotlin
.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)
```

### 4.5 의존성 추가

`app/build.gradle.kts`:
```kotlin
implementation("com.bucket4j:bucket4j-core:8.10.1")
```

---

## 5. 최종 SecurityConfig 구조

기존 `SecurityConfig.kt` 변경 요약:

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,              // 추가
    private val corsConfigurationSource: CorsConfigurationSource, // 추가
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }  // 추가
            .csrf { it.disable() }
            .headers { headers ->                                       // 추가
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { }
                    .httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true).maxAgeInSeconds(31536000)
                    }
                    .xssProtection { }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { /* 기존 설정 유지 */ }
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)  // 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
```

---

## 6. 파일 변경 목록

### 신규 파일

| 파일 | 내용 |
|------|------|
| `app/.../config/CorsConfig.kt` | CORS 전역 설정 Bean |
| `app/.../config/RateLimitConfig.kt` | Bucket4j Bandwidth Bean 설정 |
| `app/.../config/RateLimitFilter.kt` | IP 기반 Rate Limit 필터 |

### 수정 파일

| 파일 | 변경 내용 |
|------|---------|
| `app/build.gradle.kts` | `bucket4j-core:8.10.1` 의존성 추가 |
| `app/.../config/SecurityConfig.kt` | `.cors()`, `.headers()`, `rateLimitFilter` 추가 |
| `app/src/main/resources/application.properties` | `cors.allowed-origins` 키 추가 (빈 값 or placeholder) |
| `app/src/main/resources/application-local.properties` | `cors.allowed-origins=http://localhost:3000` |
| `app/src/main/resources/application-prod.properties.example` | `cors.allowed-origins=${CORS_ALLOWED_ORIGINS}` 추가 |

### 변경 없음 (기존 완료)

| 파일 | 이유 |
|------|------|
| `AuthDtos.kt`, `ProductDtos.kt`, `TransactionDtos.kt` | 유효성 어노테이션 이미 완료 |
| `*Controller.kt` (5개) | `@Valid` 이미 적용 |
| `GlobalExceptionHandler.kt` | `MethodArgumentNotValidException` 이미 처리 |
| `application-prod.properties.example` JWT | `${JWT_SECRET}` 이미 환경변수 참조 |

---

## 7. 구현 체크리스트

### Step 1 — 의존성 & 설정 (5개)

- [ ] `app/build.gradle.kts` — `bucket4j-core:8.10.1` 추가
- [ ] `application.properties` — `cors.allowed-origins=` 키 추가
- [ ] `application-local.properties` — `cors.allowed-origins=http://localhost:3000`
- [ ] `application-prod.properties.example` — `cors.allowed-origins=${CORS_ALLOWED_ORIGINS}`
- [ ] `./gradlew build -x test` — 의존성 해결 확인

### Step 2 — CORS (2개)

- [ ] `CorsConfig.kt` 신규 생성
- [ ] `SecurityConfig.kt` — `.cors()` 추가 + `CorsConfigurationSource` 주입

### Step 3 — 보안 헤더 (1개)

- [ ] `SecurityConfig.kt` — `.headers()` 블록 추가

### Step 4 — Rate Limiting (3개)

- [ ] `RateLimitConfig.kt` 신규 생성
- [ ] `RateLimitFilter.kt` 신규 생성
- [ ] `SecurityConfig.kt` — `rateLimitFilter` 주입 + `addFilterBefore` 등록

### Step 5 — 검증 (6개)

- [ ] `./gradlew build -x test` 빌드 성공
- [ ] `./gradlew :app:bootRun` 구동 성공
- [ ] `curl -X OPTIONS http://localhost:8080/api/auth/login -H "Origin: http://localhost:3000"` → `200 + Access-Control-Allow-Origin`
- [ ] `curl -I http://localhost:8080/api/auth/login` → `X-Frame-Options: DENY` 확인
- [ ] 로그인 11회 연속 → 마지막은 `429` 확인
- [ ] `./gradlew test` 기존 25개 테스트 전체 통과

---

## 8. 테스트 전략

### 단위 테스트 추가 대상

| 테스트 클래스 | 검증 내용 |
|------------|---------|
| `RateLimitFilterTest` | 제한 횟수 초과 → 429, 정상 범위 → 통과 |
| 기존 Controller 테스트 | CORS 헤더 추가 후 기존 테스트 통과 여부 |

### 기존 테스트 영향도

| 테스트 | 예상 영향 | 대응 |
|--------|---------|------|
| Controller 7개 테스트 | CORS/보안 헤더 추가로 인한 응답 헤더 변화 | 응답 바디 검증만 하므로 영향 없음 |
| Service 4개 테스트 | 없음 | - |
| Value Object 4개 테스트 | 없음 | - |
| `RateLimitFilter` | 테스트 환경에서 Filter가 활성화될 수 있음 | `@SpringBootTest` 테스트에서 Rate Limit 초과 주의 → 테스트용 높은 limit 설정 or `@Profile("!test")` |

---

## 9. 변경 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0 | 2026-03-04 | 최초 작성 — 기존 구현 재확인 후 실제 범위 확정 | pdca-design |
