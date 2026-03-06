# Archive Index — 2026-03

## controller (Phase 8: Code Review & Quality)

| 항목 | 내용 |
|------|------|
| **아카이브일** | 2026-03-06 |
| **Match Rate** | 98% |
| **브랜치** | `feature/phase8-review` |
| **경로** | `docs/archive/2026-03/controller/` |

### 포함 문서

| 파일 | 설명 |
|------|------|
| `phase8-review.plan.md` | Phase 8 계획서 |
| `phase8-review.design.md` | Phase 8 설계서 (9개 이슈 + B-1~B-5) |
| `phase8-review.analysis.md` | Phase 8 Gap Analysis |
| `phase8-review.report.md` | Phase 8 완료 보고서 |
| `controller.analysis.md` | PDCA Check — Gap Analysis (98%) |
| `controller.report.md` | PDCA 완료 보고서 (전체 로드맵 포함) |

### 주요 완료 항목

- P1: AdminController 200 응답, WishlistService RESOURCE_NOT_FOUND, GlobalExceptionHandler 핸들러 추가
- P2: 인덱스 추가, WishlistAddResponse DTO, 최대 200개 제한, @RequestBody @Valid 통일
- P3: WishlistServiceImplTest (6케이스), RateLimitFilterTest (5케이스)
- UI 피드백: B-1~B-5 DTO 필드 5건
- 추가 버그픽스: ProductType RESERVATION 복원, nearby BigDecimal 수정, HttpMessageNotReadableException 400 처리
