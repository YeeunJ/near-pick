# [Design] Phase 2.5 — Documentation & Git Workflow

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase2-workflow |
| Phase | Design |
| 작성일 | 2026-02-24 |
| 참조 | `docs/01-plan/features/phase2-workflow.plan.md` |

---

## 1. 파일 구조

```
near-pick/
├── README.md                          ← 신규
├── CONVENTIONS.md                     ← 섹션 8, 9 추가
├── .github/
│   └── PULL_REQUEST_TEMPLATE.md       ← 신규
└── docs/
    └── wiki/
        ├── 00-overview.md             ← 신규
        ├── 01-domain-glossary.md      ← 신규
        ├── 02-module-structure.md     ← 신규
        └── 03-dev-guide.md            ← 신규
```

---

## 2. README.md 구조

```markdown
# NearPick

> 지역 기반 실시간 인기 상품 커머스 플랫폼

## Overview
(프로젝트 설명, 3줄 요약)

## Tech Stack
(Spring Boot, Kotlin, Java, Gradle, H2/PostgreSQL)

## Module Structure
(4개 모듈 다이어그램 + 역할 설명)

## Getting Started
(사전 요구사항, 빌드/실행 명령어)

## Documentation
(docs/wiki/ 링크)

## Conventions
(CONVENTIONS.md 링크)
```

---

## 3. Wiki 문서 구조

### `docs/wiki/00-overview.md`

```
- 서비스 개요 (소비자 / 소상공인 / 관리자 관점)
- 핵심 기능 목록
- 비즈니스 규칙 요약
- 배송 없음 (직접 방문 수령) 정책
```

### `docs/wiki/01-domain-glossary.md`

Phase 1 스키마 기반 용어 정의. 형식:

```markdown
### 용어명
- **정의**: ...
- **영문**: ...
- **관련 Entity**: ...
- **비즈니스 규칙**: ...
```

주요 용어: 찜(Wishlist), 예약(Reservation), 선착순 구매(FlashPurchase),
인기점수(PopularityScore), 상품(Product), 소상공인(Merchant), 소비자(Consumer)

### `docs/wiki/02-module-structure.md`

```
- 4개 모듈 역할 (app / common / domain / domain-nearpick)
- 의존성 흐름 다이어그램
- 각 모듈 패키지 구조 예시
- runtimeOnly 의미와 강제 이유
```

### `docs/wiki/03-dev-guide.md`

```
- 개발 환경 설정
- 브랜치 생성 → 개발 → 커밋 → PR → 머지 플로우
- 빌드/테스트 명령어
- H2 Console 접근 방법
- 자주 겪는 문제 (FAQ)
```

---

## 4. 브랜치 컨벤션 (최종)

### 4.1 브랜치 네이밍

```
{type}/{scope-or-phase}
```

| 패턴 | 설명 | 예시 |
|------|------|------|
| `feature/phase-N-{name}` | Phase 단위 개발 | `feature/phase3-mockup` |
| `feature/{domain}-{desc}` | Phase 내 개별 기능 | `feature/product-nearby-search` |
| `fix/{desc}` | 버그 수정 | `fix/user-entity-null` |
| `docs/{desc}` | 문서만 변경 | `docs/update-readme` |
| `chore/{desc}` | 빌드/설정 | `chore/update-gradle-wrapper` |
| `refactor/{desc}` | 리팩토링 | `refactor/product-mapper` |

### 4.2 브랜치 생성 기준

- **Phase 단위**: 하나의 Phase가 하나의 `feature/phase-N-*` 브랜치
- **브랜치는 `main`에서 생성**: `git switch -c feature/phase-N-xxx`
- **머지 후 브랜치 삭제**: 로컬 + 원격 모두

---

## 5. PR 컨벤션 (최종)

### 5.1 PR 제목

```
{type}({scope}): {subject}
```

커밋 메시지와 동일한 형식. 예시:
```
feat(phase3): add UI mockups for consumer product discovery
docs(phase2): add README and wiki documentation
chore(phase2): establish branch and PR workflow conventions
```

### 5.2 PR 본문 — `.github/PULL_REQUEST_TEMPLATE.md`

```markdown
## Summary
<!-- 이 PR이 해결하는 문제 또는 추가하는 기능을 2-3줄로 요약 -->

## Changes
<!-- 변경된 주요 파일/모듈 목록 (bullet) -->

## Test Plan
- [ ] `./gradlew build` 통과
- [ ] (추가 검증 방법)

## Related
<!-- 관련 문서, Phase, 이슈 링크 -->

## Checklist
- [ ] 커밋 메시지가 컨벤션을 준수함 (`{type}({scope}): {subject}`)
- [ ] `./gradlew build` 성공 확인
- [ ] CONVENTIONS.md 위반 없음
```

### 5.3 PR 규칙

| 규칙 | 내용 |
|------|------|
| 브랜치 | `main`으로만 PR (feature → main) |
| 제목 | 커밋 컨벤션 형식 준수 |
| 본문 | 템플릿 모든 섹션 작성 |
| 빌드 | PR 생성 전 `./gradlew build` 로컬 통과 확인 |
| 스쿼시 | 기본 머지 (스쿼시 X — 커밋 히스토리 보존) |

---

## 6. CONVENTIONS.md 추가 섹션

### 섹션 8. 브랜치 전략 (신규)

```
브랜치 전략, 네이밍 규칙, Phase 브랜치 설명
```

### 섹션 9. PR 컨벤션 (신규)

```
PR 제목 형식, PR 본문 구조, PR 규칙
```

---

## 7. 완료 기준 체크

- [ ] `README.md` 작성 완료
- [ ] `docs/wiki/00-overview.md`
- [ ] `docs/wiki/01-domain-glossary.md`
- [ ] `docs/wiki/02-module-structure.md`
- [ ] `docs/wiki/03-dev-guide.md`
- [ ] `.github/PULL_REQUEST_TEMPLATE.md`
- [ ] `CONVENTIONS.md` 섹션 8, 9 추가
- [ ] `feature/phase2-workflow` 브랜치에서 작업
- [ ] 커밋 메시지 컨벤션 준수
- [ ] PR 생성 (템플릿 준수) → main 머지

---

## 8. 다음 단계

`/pdca do phase2-workflow` → 실제 구현 시작
