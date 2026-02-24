# [Plan] Phase 2.5 — Documentation & Git Workflow

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase2-workflow |
| Phase | Plan |
| 작성일 | 2026-02-24 |
| 프로젝트 | NearPick |
| 레벨 | Enterprise |
| 참조 | Phase 2 컨벤션 기반 |

---

## 1. 목표 (Objective)

1. 프로젝트 진입장벽을 낮추는 **README.md** 및 **Wiki** 문서를 작성한다.
2. Phase 3부터 모든 개발을 **브랜치 → PR → 머지** 방식으로 관리하는 규칙을 확립하고 CONVENTIONS.md에 반영한다.
3. 이번 Phase 자체가 첫 번째 PR 실습이 된다 (`feature/phase2-workflow` → `main`).

---

## 2. 배경 (Background)

- Phase 1(스키마), Phase 2(컨벤션)가 완료되어 프로젝트 구조와 규칙이 확립됨
- 새 팀원/AI가 프로젝트를 이해할 수 있는 진입 문서가 아직 없음
- Phase 3부터는 기능 개발이 시작되므로 PR 기반 워크플로우를 미리 정립해야 함
- 커밋/PR 컨벤션이 없으면 히스토리 추적과 코드 리뷰가 어려워짐

---

## 3. 범위 (Scope)

### In Scope
- `README.md` 작성 (프로젝트 개요, 구조, 설치/실행 방법)
- Wiki 문서 작성 (`docs/wiki/` 디렉토리)
  - 도메인 용어 사전 (Phase 1 기반)
  - 모듈 구조 설명 (Phase 2 기반)
  - 개발 가이드 (이번 Phase 기반)
- 브랜치 네이밍 컨벤션 정의
- PR 템플릿 작성 (`.github/PULL_REQUEST_TEMPLATE.md`)
- CONVENTIONS.md에 브랜치/PR 섹션 추가
- 이번 Phase 자체를 첫 PR로 만들어 새 워크플로우 실습

### Out of Scope
- GitHub Actions / CI 자동화 (Phase 9)
- Issue 템플릿 (Phase 9)
- 코드 리뷰 자동화 (Phase 9)

---

## 4. 브랜치 전략 (안)

```
main                  ← 항상 빌드 가능한 안정 브랜치
└── feature/phase-N-{name}  ← Phase 단위 개발 브랜치
    └── 완료 시 PR → main 머지
```

### 브랜치 네이밍 규칙

| 패턴 | 용도 | 예시 |
|------|------|------|
| `feature/phase-N-{name}` | Phase 단위 기능 개발 | `feature/phase3-mockup` |
| `fix/{issue-or-desc}` | 버그 픽스 | `fix/user-entity-null` |
| `docs/{desc}` | 문서 변경만 | `docs/update-readme` |
| `chore/{desc}` | 빌드/설정 변경 | `chore/update-gradle` |

---

## 5. PR 컨벤션 (안)

### PR 제목 형식

```
{type}({scope}): {subject}
```

커밋 메시지와 동일한 형식 (CONVENTIONS.md 섹션 7 기준).

### PR 본문 구조

```markdown
## Summary
<!-- 변경 사항 요약 (2-3 bullet) -->

## Changes
<!-- 주요 변경 파일/모듈 목록 -->

## Test Plan
<!-- 검증 방법 -->

## Related
<!-- 관련 문서/이슈 링크 -->
```

---

## 6. 작업 목록 (Tasks)

- [ ] `feature/phase2-workflow` 브랜치 생성
- [ ] `README.md` 작성
- [ ] `docs/wiki/` 디렉토리 + 문서 작성
  - [ ] `00-overview.md` — 프로젝트 개요
  - [ ] `01-domain-glossary.md` — 도메인 용어사전
  - [ ] `02-module-structure.md` — 모듈 구조 가이드
  - [ ] `03-dev-guide.md` — 개발 참여 가이드
- [ ] `.github/PULL_REQUEST_TEMPLATE.md` 작성
- [ ] `CONVENTIONS.md` — 브랜치/PR 섹션 추가
- [ ] 커밋 후 PR 생성 → main 머지

---

## 7. 완료 기준 (Definition of Done)

- [ ] `README.md` 존재, 내용 충실 (프로젝트 개요/구조/실행 방법 포함)
- [ ] `docs/wiki/` 에 4개 문서 존재
- [ ] `.github/PULL_REQUEST_TEMPLATE.md` 존재
- [ ] `CONVENTIONS.md` 브랜치/PR 섹션 추가됨
- [ ] `feature/phase2-workflow` 브랜치에서 커밋이 컨벤션을 준수함
- [ ] PR이 PR 템플릿을 준수하여 생성됨
- [ ] PR이 main에 머지됨

---

## 8. 다음 단계

완료 후 → `/pdca design phase2-workflow`
