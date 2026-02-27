# [Plan] Phase 5 — Design System (Next.js + shadcn/ui)

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase5-design-system |
| Phase | Plan |
| 작성일 | 2026-02-27 |
| 참조 | `docs/02-design/features/phase3-mockup.design.md` |

---

## 배경 및 목적

Phase 4에서 24개 API 백엔드 구현이 완료됐다. Phase 5에서는 사용자가 실제로 접하는
프론트엔드 레이어를 구축한다. Phase 3 목업(13개 화면)을 기반으로 Next.js + shadcn/ui로
디자인 시스템을 확립하고, 모든 화면의 정적 UI를 구현한다.
API 연동은 Phase 6에서 처리한다.

### 왜 지금인가?
- 백엔드 API가 완성된 상태 → 프론트엔드 개발에 필요한 계약(Contract) 확정됨
- Phase 3 목업이 상세하게 설계됨 → 화면 구조 결정 없이 바로 구현 진입 가능
- 디자인 시스템 먼저 확립 → Phase 6 API 연동 시 UI 완성도 유지

---

## 목표 (Goal)

1. **Next.js 프로젝트 초기화**
   - `web/` 디렉토리 (현 모노레포 루트 아래)
   - TypeScript + App Router + Tailwind CSS + shadcn/ui

2. **디자인 토큰 정의**
   - 색상, 타이포그래피, 간격, 반경 (NearPick 브랜드 기반)
   - Tailwind config에 CSS Custom Property로 관리

3. **공통 컴포넌트 구축**
   - shadcn/ui 기본 컴포넌트 설치 + NearPick 커스텀 래퍼
   - 레이아웃 컴포넌트 (Header, BottomNav, Sidebar, PageContainer)

4. **전체 13개 화면 정적 UI 구현**
   - AUTH: 로그인, 회원가입 (2개)
   - CON: 홈, 상품 상세, 예약, 선착순 구매, 마이페이지(찜·예약·구매) (7개)
   - MER: 대시보드, 상품 등록, 상품 목록, 예약 관리 (4개)
   - ADM: 사용자 관리, 상품 검수 (2개)
   > 총 15개 라우트 (일부 화면이 탭/서브 페이지로 분리)

---

## 범위 (Scope)

### In Scope
| 항목 | 설명 |
|------|------|
| Next.js 프로젝트 생성 | `web/` — App Router, TypeScript |
| Tailwind + 디자인 토큰 | NearPick 컬러·타이포 정의 |
| shadcn/ui 컴포넌트 설치 | Button, Card, Input, Badge, Dialog, Table 등 |
| 공통 레이아웃 | Header, BottomNav(모바일), Sidebar(대시보드) |
| AUTH 화면 2개 | 로그인, 회원가입 |
| CON 화면 7개 | 홈, 상품 상세, 예약, 선착순 구매, 찜/예약/구매 내역 |
| MER 화면 4개 | 대시보드, 상품 등록, 상품 목록, 예약 관리 |
| ADM 화면 2개 | 사용자 관리, 상품 검수 |
| Mock 데이터 | 정적 fixtures — API 연동 전 UI 확인용 |

### Out of Scope
- 실제 API 호출 (Phase 6)
- 인증 상태 관리 / 로그인 세션 (Phase 6)
- 실시간 기능 / WebSocket (Phase 7+)
- 테스트 코드 (Phase 8)
- SEO 최적화 (Phase 7)

---

## 기술 선택

| 항목 | 선택 | 이유 |
|------|------|------|
| 프레임워크 | Next.js 15 (App Router) | SEO 필요한 소비자 화면 + RSC 지원 |
| 언어 | TypeScript | 타입 안전성, 백엔드 DTO와 타입 싱크 |
| 스타일링 | Tailwind CSS v4 | 유틸리티 기반, 디자인 토큰 관리 용이 |
| 컴포넌트 | shadcn/ui | 코드 소유권 방식, Radix UI 기반 접근성 |
| 패키지 매니저 | pnpm | 모노레포 친화적, 빠른 설치 |
| 상태 관리 | 없음 (Phase 5는 정적 UI) | Phase 6에서 필요 시 Zustand/TanStack Query 도입 |
| 아이콘 | lucide-react | shadcn/ui 기본 아이콘 세트 |

---

## 프로젝트 구조

```
web/                                    # Next.js 프로젝트 루트
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx              # AUTH-01
│   │   └── signup/page.tsx             # AUTH-02
│   ├── (consumer)/
│   │   ├── layout.tsx                  # 소비자 레이아웃 (Header + BottomNav)
│   │   ├── page.tsx                    # CON-01: 홈 (근처 상품 목록)
│   │   ├── products/[id]/page.tsx      # CON-02: 상품 상세
│   │   ├── products/[id]/reserve/      # CON-03: 예약하기
│   │   ├── products/[id]/purchase/     # CON-04: 선착순 구매
│   │   └── mypage/
│   │       ├── wishlist/page.tsx       # CON-05a: 찜 목록
│   │       ├── reservations/page.tsx   # CON-05b: 예약 내역
│   │       └── purchases/page.tsx      # CON-05c: 구매 내역
│   ├── (merchant)/
│   │   ├── layout.tsx                  # 소상공인 레이아웃 (Sidebar)
│   │   ├── dashboard/page.tsx          # MER-01: 대시보드
│   │   ├── products/
│   │   │   ├── page.tsx                # MER-03: 상품 목록
│   │   │   └── new/page.tsx            # MER-02: 상품 등록
│   │   └── reservations/page.tsx       # MER-04: 예약 관리
│   └── (admin)/
│       ├── layout.tsx                  # 관리자 레이아웃 (Sidebar)
│       ├── users/page.tsx              # ADM-01: 사용자 관리
│       └── products/page.tsx           # ADM-02: 상품 검수
├── components/
│   ├── ui/                             # shadcn/ui 기본 컴포넌트 (자동 생성)
│   ├── layout/                         # Header, BottomNav, Sidebar, PageContainer
│   └── features/                       # 화면별 커스텀 컴포넌트
│       ├── product/                    # ProductCard, ProductMap, NearbyFilter
│       ├── reservation/                # ReservationItem, StatusBadge
│       ├── merchant/                   # DashboardStat, ProductForm
│       └── admin/                      # UserTable, ProductReviewCard
├── lib/
│   ├── utils.ts                        # cn() 등 유틸
│   └── mock/                           # Mock 데이터 fixtures
│       ├── products.ts
│       ├── reservations.ts
│       └── users.ts
└── types/
    └── api.ts                          # 백엔드 DTO와 싱크된 TypeScript 타입
```

---

## 디자인 토큰 방향

NearPick은 **"근처", "따뜻함", "신뢰"** 키워드의 로컬 커머스 서비스.

| 토큰 | 값 (예정) | 용도 |
|------|----------|------|
| `--color-primary` | `#FF6B35` (오렌지) | CTA 버튼, 강조 |
| `--color-secondary` | `#2D6A4F` (초록) | 근처/지역 느낌 |
| `--color-surface` | `#F8F7F4` | 배경 |
| `--color-text` | `#1A1A1A` | 본문 |
| `font-sans` | Pretendard | 한국어 최적화 |
| `radius` | `0.75rem` | 카드/버튼 모서리 |

---

## 구현 순서

```
[프로젝트 설정]
1. pnpm create next-app web (TypeScript, App Router, Tailwind)
2. shadcn/ui init + 기본 컴포넌트 설치 (button, card, input, badge, dialog, table, tabs)
3. Pretendard 폰트 설정
4. 디자인 토큰 (tailwind.config.ts + globals.css CSS variables)

[공통 컴포넌트]
5. 레이아웃 컴포넌트: Header, BottomNav (소비자), Sidebar (소상공인/관리자)
6. 공통 컴포넌트: PageContainer, LoadingSpinner, EmptyState, StatusBadge

[Mock 데이터]
7. lib/mock/ — products, reservations, users 픽스처 작성
8. types/api.ts — 백엔드 DTO 기반 TypeScript 타입 정의

[AUTH 화면]
9. 로그인 (AUTH-01)
10. 회원가입 (AUTH-02: Consumer / Merchant 분기)

[소비자 화면]
11. CON-01: 홈 — ProductCard 그리드, 위치 필터 UI
12. CON-02: 상품 상세 — 이미지, 정보, 찜/예약/구매 버튼
13. CON-03: 예약하기 — 날짜/수량 입력 폼
14. CON-04: 선착순 구매 — 재고 표시, 구매 확인 다이얼로그
15. CON-05: 마이페이지 — 탭(찜/예약/구매 내역)

[소상공인 화면]
16. MER-01: 대시보드 — 통계 카드, 최근 예약
17. MER-02: 상품 등록 폼
18. MER-03: 상품 목록 — 테이블, 상태 필터
19. MER-04: 예약 관리 — 예약 목록, 확정/거절 액션

[관리자 화면]
20. ADM-01: 사용자 관리 — 테이블, 검색, 정지 액션
21. ADM-02: 상품 검수 — 목록, 강제 종료 액션

[검증]
22. 전체 라우트 접근 가능 확인 (pnpm dev)
23. 반응형(모바일/태블릿/데스크톱) 확인
```

---

## 주요 산출물

| 파일/디렉토리 | 설명 |
|---------------|------|
| `web/` | Next.js 프로젝트 전체 |
| `web/app/(auth)/` | 로그인, 회원가입 페이지 |
| `web/app/(consumer)/` | 소비자 7개 화면 |
| `web/app/(merchant)/` | 소상공인 4개 화면 |
| `web/app/(admin)/` | 관리자 2개 화면 |
| `web/components/ui/` | shadcn/ui 컴포넌트 |
| `web/components/layout/` | 공통 레이아웃 |
| `web/components/features/` | 화면별 커스텀 컴포넌트 |
| `web/lib/mock/` | Mock 데이터 fixtures |
| `web/types/api.ts` | 백엔드 DTO 기반 TS 타입 |

---

## 성공 기준

- [ ] `pnpm dev` 실행 → `localhost:3000` 정상 접근
- [ ] 전체 15개 라우트 200 응답 (에러 페이지 없음)
- [ ] 모바일(375px) / 태블릿(768px) / 데스크톱(1280px) 레이아웃 정상
- [ ] Mock 데이터로 모든 화면에 실제 데이터 형태 표시
- [ ] 공통 컴포넌트 재사용 — 중복 마크업 없음
- [ ] TypeScript 컴파일 에러 없음 (`pnpm build` 성공)
- [ ] Gap Analysis Match Rate ≥ 90%

---

## 리스크

| 리스크 | 대응 |
|--------|------|
| Tailwind v4 shadcn/ui 호환성 | shadcn/ui 최신 버전 확인, 필요 시 Tailwind v3 유지 |
| Pretendard 폰트 로딩 최적화 | `next/font`의 `localFont` 사용 |
| 소비자 지도 UI (위치 기반 홈) | Phase 5에서는 지도 없이 리스트만, Phase 6에서 Kakao/Naver Map 연동 |
| 관리자/소상공인 인증 분기 | Phase 5는 정적이므로 URL 직접 접근; Phase 6에서 미들웨어로 보호 |
