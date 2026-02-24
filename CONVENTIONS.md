# NearPick Coding Conventions

> 이 문서는 NearPick 프로젝트의 모든 코드 작성 기준입니다.
> AI와 개발자 모두 이 규칙을 따릅니다.

---

## 1. 멀티모듈 구조

```
app             ← Controller, 진입점, 보안 설정
common          ← 공통 응답/예외/유틸 (독립)
domain          ← Service 인터페이스, Data class, DTO (JPA 없음)
domain-nearpick ← Service 구현체, JPA Repository, Entity, Mapper
```

### 의존성 규칙

```
app ──(compile)──────> domain ──(compile)──> common
 └──(runtimeOnly)──> domain-nearpick ──> domain, common
```

- `app`은 `domain-nearpick`을 `runtimeOnly`로만 의존
  - **Controller에서 Repository / Entity 직접 import 불가** (컴파일 에러)
  - Service는 반드시 `domain`의 인터페이스를 통해서만 접근
- `domain`은 JPA 의존성을 가지지 않음

---

## 2. 네이밍 규칙

### 클래스 / 인터페이스

| 종류 | 규칙 | 예시 |
|------|------|------|
| Service 인터페이스 | `{Domain}Service` | `ProductService` |
| Service 구현체 | `{Domain}ServiceImpl` | `ProductServiceImpl` |
| JPA Repository | `{Domain}Repository` | `ProductRepository` |
| JPA Entity | `{Domain}Entity` | `ProductEntity` |
| 순수 도메인 모델 | `{Domain}` (접미사 없음) | `Product`, `User` |
| Mapper | `{Domain}Mapper` | `ProductMapper` |
| Controller | `{Domain}Controller` | `ProductController` |
| Request DTO | `{Action}{Domain}Request` | `CreateProductRequest` |
| Response DTO | `{Domain}Response` | `ProductResponse` |
| 예외 클래스 | `{Name}Exception` | `ProductNotFoundException` |
| 설정 클래스 | `{Name}Config` | `SecurityConfig` |

### 기타

| 대상 | 규칙 | 예시 |
|------|------|------|
| Enum 클래스 | PascalCase | `UserRole`, `ProductStatus` |
| Enum 값 | UPPER_SNAKE_CASE | `FLASH_SALE`, `FORCE_CLOSED` |
| 함수/변수 | camelCase | `findNearbyProducts()`, `shopLat` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RADIUS_KM` |
| 패키지 | lowercase 단수형 | `com.nearpick.domain.product` |

---

## 3. 패키지 구조

### `domain` 모듈
```
com.nearpick.domain.{domain}/
├── {Domain}Service.kt          # interface
├── {DomainName}.kt             # data class (순수 도메인 모델)
└── dto/
    ├── request/
    │   └── {Action}{Domain}Request.kt
    └── response/
        └── {Domain}Response.kt
```

### `domain-nearpick` 모듈
```
com.nearpick.nearpick.{domain}/
├── {Domain}ServiceImpl.kt      # implements {Domain}Service
├── {Domain}Repository.kt       # : JpaRepository<{Domain}Entity, Long>
├── {Domain}Entity.kt           # @Entity
└── {Domain}Mapper.kt           # Entity ↔ domain model
```

### `app` 모듈
```
com.nearpick.app/
├── NearPickApplication.kt
├── config/
└── {domain}/
    └── {Domain}Controller.kt
```

### `common` 모듈
```
com.nearpick.common/
├── exception/
│   ├── BusinessException.kt
│   └── ErrorCode.kt
└── response/
    └── ApiResponse.kt
```

---

## 4. JPA Entity 규칙

1. `data class` 사용 금지 → 일반 `class` 사용
   - JPA 프록시 생성, `equals`/`hashCode` 오동작, `toString` 순환 참조 방지
2. `id`: `Long` 타입, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
3. Enum: `@Enumerated(EnumType.STRING)` 필수 (숫자 저장 금지)
4. 연관관계: 기본 `FetchType.LAZY`
5. `createdAt`: `updatable = false`
6. 양방향 연관관계 지양 → 단방향 우선
7. `equals()`/`hashCode()` 오버라이드 시 `id`만 사용
8. Entity 클래스는 `domain-nearpick` 모듈에만 위치

---

## 5. Spring 계층 규칙

| 계층 | 위치 | 책임 |
|------|------|------|
| Controller | `app` | `@Valid` 검사, Service 호출, DTO 반환 |
| Service (interface) | `domain` | 비즈니스 계약 정의 |
| Service (impl) | `domain-nearpick` | `@Transactional`, 비즈니스 로직, Repository 호출 |
| Repository | `domain-nearpick` | `JpaRepository` 확장, `@Query` |

- Controller → Service 인터페이스만 의존 (구현체 직접 의존 금지)
- Service → Repository (Service끼리 직접 호출 지양, 필요 시 같은 계층 내에서만)

---

## 6. 예외 처리 규칙

- 커스텀 예외: `common` 모듈 `exception/` 패키지
- 비즈니스 예외: `RuntimeException` 상속
- 전역 처리: `app` 모듈 `@RestControllerAdvice`
- 예외 메시지: 영문 (로그), 사용자 응답은 `ErrorCode`로 관리

---

## 7. Git 커밋 규칙

```
{type}({scope}): {subject}
```

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `refactor` | 리팩토링 |
| `test` | 테스트 |
| `chore` | 빌드/설정 변경 |

scope: `app` | `common` | `domain` | `domain-nearpick`

예시: `feat(domain-nearpick): add ProductServiceImpl with nearby search`

---

## 8. 브랜치 전략

### 브랜치 구조

```
main                              ← 항상 빌드 가능한 안정 브랜치
└── feature/phase-N-{name}        ← Phase 단위 작업 브랜치
```

- `main`은 직접 커밋 금지 — 반드시 PR을 통해 머지
- 브랜치는 항상 최신 `main`에서 생성
- 머지 완료 후 브랜치 삭제 (로컬 + 원격)

### 브랜치 네이밍

| 패턴 | 용도 | 예시 |
|------|------|------|
| `feature/phase-N-{name}` | Phase 단위 개발 | `feature/phase3-mockup` |
| `feature/{domain}-{desc}` | Phase 내 개별 기능 | `feature/product-nearby-search` |
| `fix/{desc}` | 버그 수정 | `fix/user-entity-null` |
| `docs/{desc}` | 문서만 변경 | `docs/update-readme` |
| `chore/{desc}` | 빌드/설정 변경 | `chore/update-gradle-wrapper` |
| `refactor/{desc}` | 리팩토링 | `refactor/product-mapper` |

---

## 9. PR 컨벤션

### PR 제목

커밋 메시지와 동일한 형식:

```
{type}({scope}): {subject}
```

예시:
```
feat(phase3): add UI mockups for consumer product discovery
docs: add README and wiki documentation
chore(phase2): establish branch and PR workflow conventions
```

### PR 규칙

| 규칙 | 내용 |
|------|------|
| 대상 브랜치 | `main`으로만 PR |
| 제목 | 커밋 컨벤션 형식 준수 |
| 본문 | `.github/PULL_REQUEST_TEMPLATE.md` 모든 섹션 작성 |
| 빌드 | PR 생성 전 `./gradlew build` 로컬 통과 확인 |
| 머지 방식 | Merge commit (스쿼시 X — 커밋 히스토리 보존) |

### PR 본문 구조 (`.github/PULL_REQUEST_TEMPLATE.md`)

```markdown
## Summary        ← 2-3줄 요약
## Changes        ← 변경 파일/모듈 목록
## Test Plan      ← 빌드 통과 + 추가 검증
## Related        ← 관련 Phase, 문서, 이슈
## Checklist      ← 커밋 컨벤션, 빌드, CONVENTIONS 준수 확인
```
