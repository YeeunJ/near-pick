# 개발 참여 가이드

NearPick 프로젝트의 개발 환경 설정과 Git 워크플로우 안내.

---

## 개발 환경 설정

### 사전 요구사항

| 도구 | 버전 | 비고 |
|------|------|------|
| JDK | 17+ | Temurin(Eclipse) 권장 |
| Gradle | 자동 (Wrapper 사용) | `./gradlew` 사용 |
| IDE | IntelliJ IDEA | Kotlin 공식 지원 |
| Git | 2.x+ | |

### 프로젝트 클론 및 빌드

```bash
git clone {repo-url}
cd near-pick
./gradlew build
```

### IntelliJ IDEA 설정

1. **File → Open** → `near-pick/` 폴더 선택
2. Gradle 프로젝트로 자동 임포트됨
3. **Project SDK**: Java 17 설정 확인
4. **Build Tool**: Gradle → `gradle-wrapper.properties` 사용

---

## 브랜치 워크플로우

### 전체 흐름

```
main
 └── feature/phase-N-{name}   ← Phase 단위 작업
      ↓ PR 생성
     main (머지)
```

### 브랜치 생성

```bash
# main에서 새 브랜치 생성
git switch main
git pull
git switch -c feature/phase3-mockup
```

### 브랜치 네이밍 규칙

| 패턴 | 용도 | 예시 |
|------|------|------|
| `feature/phase-N-{name}` | Phase 단위 개발 | `feature/phase3-mockup` |
| `feature/{domain}-{desc}` | Phase 내 개별 기능 | `feature/product-nearby-search` |
| `fix/{desc}` | 버그 수정 | `fix/user-entity-null` |
| `docs/{desc}` | 문서만 변경 | `docs/update-readme` |
| `chore/{desc}` | 빌드/설정 변경 | `chore/update-gradle-wrapper` |
| `refactor/{desc}` | 리팩토링 | `refactor/product-mapper` |

---

## 커밋 컨벤션

형식: `{type}({scope}): {subject}`

### type

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드/설정 변경 |

### scope

`app` | `common` | `domain` | `domain-nearpick`

### 예시

```bash
feat(domain-nearpick): add ProductServiceImpl with nearby search
fix(domain): fix null handling in ConsumerProfile
docs(app): add API endpoint documentation
chore: update gradle wrapper to 9.4
refactor(domain-nearpick): extract mapper logic to separate class
```

---

## PR 워크플로우

### 1. 로컬 빌드 확인

```bash
./gradlew build
```

빌드 실패 시 PR 생성 금지.

### 2. 커밋 및 푸시

```bash
git add {파일들}
git commit -m "feat(domain-nearpick): add ProductServiceImpl"
git push -u origin feature/phase3-mockup
```

### 3. PR 생성

GitHub → **New Pull Request**

- **Base**: `main`
- **Compare**: 작업 브랜치
- **제목**: 커밋 컨벤션 형식 (`{type}({scope}): {subject}`)
- **본문**: PR 템플릿 모든 섹션 작성

### 4. PR 본문 템플릿 (`.github/PULL_REQUEST_TEMPLATE.md`)

```markdown
## Summary
<!-- 변경 사항 2-3줄 요약 -->

## Changes
<!-- 변경된 주요 파일/모듈 -->

## Test Plan
- [ ] `./gradlew build` 통과

## Related
<!-- 관련 Phase, 문서 링크 -->

## Checklist
- [ ] 커밋 메시지 컨벤션 준수
- [ ] `./gradlew build` 성공 확인
- [ ] CONVENTIONS.md 위반 없음
```

### 5. 머지 후 브랜치 삭제

```bash
git switch main
git pull
git branch -d feature/phase3-mockup          # 로컬 삭제
git push origin --delete feature/phase3-mockup  # 원격 삭제
```

---

## 자주 사용하는 명령어

```bash
# 전체 빌드
./gradlew build

# 테스트만 실행
./gradlew test

# 빌드 캐시 정리 후 재빌드
./gradlew clean build

# 특정 모듈만 빌드
./gradlew :domain-nearpick:build

# 앱 실행
./gradlew :app:bootRun
```

---

## H2 Console (개발 환경)

앱 실행 후 접근:

- URL: <http://localhost:8080/h2-console>
- JDBC URL: `jdbc:h2:mem:nearpick`
- Username: `sa`
- Password: (비워두기)

---

## 자주 겪는 문제

### `runtimeOnly` 관련 컴파일 에러

```
error: unresolved reference: ProductRepository
```

**원인**: `app` 모듈에서 `domain-nearpick`의 클래스를 직접 import
**해결**: `domain`의 Service 인터페이스를 통해 접근 (설계 위반 수정)

### Gradle 빌드 실패 - 플러그인 없음

```
Plugin [id: '...'] was not found
```

**해결**: `./gradlew wrapper --gradle-version 9.3` 실행 후 재빌드

### H2 Console 접근 불가

**원인**: Spring Security가 기동되면 H2 Console이 차단될 수 있음
**해결**: `application.properties`에 H2 Console 경로 예외 처리 확인
