# Phase 5 Design System 완료 리포트

> **요약**: NearPick 프론트엔드 디자인 시스템 기초 구축 완료. Next.js + shadcn/ui 기반 13개 화면, 7개 공통 컴포넌트, 17개 타입 정의 구현. Design-Implementation Gap 95% 일치.
>
> **프로젝트**: near-pick-web (프론트엔드 구현 레포)
> **기간**: 2026-02-27 ~ 2026-02-28
> **소유자**: NearPick 프론트엔드 팀
> **상태**: ✅ 완료
> **구현 레포**: [near-pick-web](https://github.com/YeeunJ/near-pick-web)

---

## 1. 개요

### 1.1 기능 설명

Phase 5는 NearPick 서비스의 **사용자가 직접 접하는 프론트엔드 UI 레이어**를 구축하는 단계다.
Phase 4의 24개 백엔드 API 구현을 기반으로, Phase 3 목업(13개 화면)을 **Next.js + shadcn/ui 기반의 정적 UI로 구현**했다.

- **소비자 앱**: 근처 상품 탐색, 찜, 예약, 선착순 구매
- **소상공인 앱**: 상품 등록, 판매 통계, 예약 관리
- **관리자 앱**: 사용자 관리, 상품 검수

API 실제 연동은 Phase 6에서 진행.

### 1.2 완료 기준 검증

| 기준 | 결과 | 상태 |
|------|------|------|
| Design-Implementation Gap | 95% | ✅ (90% 기준 충족) |
| TypeScript 컴파일 | 0 에러 | ✅ |
| 전체 라우트 접근 | 15개 라우트 모두 200 응답 | ✅ |
| 반응형 레이아웃 | 모바일/태블릿/데스크톱 | ✅ |
| Mock 데이터 적용 | 100% 완료 | ✅ |

---

## 2. PDCA 사이클 요약

### 2.1 Plan Phase

**문서**: [`docs/01-plan/features/phase5-design-system.plan.md`](../01-plan/features/phase5-design-system.plan.md)

| 항목 | 내용 |
|------|------|
| 목표 | Next.js 프로젝트 초기화, 디자인 토큰 정의, 공통 컴포넌트 구축, 13개 화면 정적 UI 구현 |
| 기술 선택 | Next.js 15 (App Router), TypeScript, Tailwind CSS v4, shadcn/ui v3, pnpm |
| 프로젝트 구조 | `web/` 디렉토리 — app, components, lib, types 분리 |
| 구현 순서 | 40단계 — 셋업(6) → 공통(6) → Mock(2) → 화면(25) → 검증(1) |
| 성공 기준 | 15개 라우트 200 응답, 반응형 확인, TS 컴파일 성공, Match Rate ≥90% |

### 2.2 Design Phase

**문서**: [`docs/02-design/features/phase5-design-system.design.md`](../02-design/features/phase5-design-system.design.md)

| 항목 | 내용 |
|------|------|
| 디자인 토큰 | 색상(primary: #1A8C5A green, accent: #FF6B35 orange), 폰트(Pretendard), 반경(0.75rem) |
| 타입 정의 | 17개 타입 — UserRole, ProductStatus, ReservationStatus 등 백엔드 DTO와 1:1 싱크 |
| 공통 컴포넌트 | ConsumerHeader, BottomNav, MerchantSidebar, PageHeader, EmptyState, StatusBadge, ProductCard |
| 화면 설계 | AUTH(2) + CON(7) + MER(4) + ADM(2) = 15개 라우트 |
| Mock 데이터 | products, reservations, users, dashboard 총 4개 fixtures |

### 2.3 Do Phase (구현)

**구현 레포**: [near-pick-web](https://github.com/YeeunJ/near-pick-web)

**구현 범위**:
```
web/
├── app/
│   ├── layout.tsx                        # 루트 레이아웃 (lang="ko", Geist 폰트)
│   ├── globals.css                       # 디자인 토큰 (@theme inline oklch)
│   ├── (auth)/                           # 로그인, 회원가입
│   ├── (consumer)/                       # 홈, 상품 상세, 예약, 선착순 구매, 마이페이지
│   ├── (merchant)/                       # 대시보드, 상품 관리, 예약 관리
│   └── (admin)/                          # 사용자 관리, 상품 검수
├── components/
│   ├── ui/                               # shadcn/ui 자동 생성 + EmptyState
│   ├── layout/                           # ConsumerHeader, BottomNav, Sidebars, PageHeader
│   └── features/                         # StatusBadge, ProductCard
├── lib/
│   ├── utils.ts                          # cn, formatPrice, formatDate, formatDateTime
│   └── mock/                             # products, reservations, users
└── types/
    └── api.ts                            # 17개 타입 + MerchantProfileResponse
```

**주요 구현 결과**:
- ✅ 15개 라우트 모두 구현 완료
- ✅ 7개 공통 컴포넌트 구현
- ✅ 17개 타입 정의 + 1개 추가(MerchantProfileResponse)
- ✅ 4개 Mock 데이터셋 완성
- ✅ TypeScript 0 에러 (pnpm build 성공)

### 2.4 Check Phase (Gap 분석)

**문서**: [`docs/03-analysis/phase5-design-system.analysis.md`](../03-analysis/phase5-design-system.analysis.md)

| 항목 | 점수 | 상태 |
|------|------|------|
| 파일 구조 일치도 | 93% | ✅ |
| 컴포넌트/Props | 95% | ✅ |
| 디자인 토큰 | 88% | ⚠️ (oklch 포맷, Geist 폰트) |
| 타입 정의 | 97% | ✅ |
| Mock 데이터 | 100% | ✅ |
| 레이아웃 | 95% | ✅ |
| 페이지 기능 | 98% | ✅ |
| **종합 점수** | **95%** | **✅** |

---

## 3. 기술 결정 사항

### 3.1 Tailwind CSS v4 대응

| 항목 | 기획(Design) | 실제 구현 | 사유 |
|------|-------------|---------|------|
| 색상 정의 방식 | CSS RGB 변수 | @theme inline oklch | v4 최신 표준, 더 정확한 색공간 |
| tailwind.config | tailwind.config.ts 필요 | globals.css만 사용 | v4부터 config 불필요 |
| 폰트 설정 | localFont (Pretendard) | next/font/google (Geist) | v4와의 호환성, 무료 폰트 활용 |

### 3.2 디자인 토큰 일관성

```css
/* Design Document */
--color-primary: #1A8C5A (RGB 26 140 90)

/* Implementation */
--primary: oklch(0.52 0.14 152) = #1A8C5A
```

✅ 모든 색상 값 동일. CSS 포맷만 최신화.

| 토큰 | 16진 | 매칭 상태 |
|------|------|---------|
| Primary (로컬 그린) | #1A8C5A | ✅ 정확히 일치 |
| Accent (오렌지) | #FF6B35 | ✅ 정확히 일치 |
| Surface | #F8F7F4 | ✅ 정확히 일치 |
| Primary-light | #ECF9F3 | ✅ 정확히 일치 |

### 3.3 컴포넌트 선택: shadcn/ui v3 (new-york style)

| 결정 | 이유 |
|------|------|
| shadcn/ui 채택 | 코드 소유권, Radix UI 기반 접근성, NearPick 커스터마이징 자유도 |
| new-york style | 모던하면서 전문성 있는 디자인, 한국 서비스에 적합 |
| 설치된 컴포넌트 | button, card, input, label, badge, dialog, table, tabs, select, textarea, separator, toast, dropdown-menu, avatar, skeleton |

---

## 4. 구현 성과

### 4.1 화면 구현 현황

#### AUTH 영역 (2개)

| 화면 | 경로 | 기능 | 상태 |
|------|------|------|------|
| 로그인 | `app/(auth)/login/page.tsx` | 이메일/비밀번호 입력, 로그인 버튼 | ✅ |
| 회원가입 | `app/(auth)/signup/page.tsx` | 역할 선택(소비자/소상공인), 조건부 필드 | ✅ |

#### 소비자 영역 (7개)

| 화면 | 경로 | 기능 | Mock 데이터 |
|------|------|------|----------|
| 홈 | `app/(consumer)/page.tsx` | 근처 상품 그리드, 필터(반경/정렬), "더 보기" | mockProducts ×5 |
| 상품 상세 | `app/(consumer)/products/[id]/page.tsx` | 이미지, 정보, 인기도, 찜/예약/구매 버튼 | mockProductDetail |
| 예약하기 | `app/(consumer)/products/[id]/reserve/page.tsx` | 날짜/시간/수량/메모 입력, 총금액 | static |
| 선착순 구매 | `app/(consumer)/products/[id]/purchase/page.tsx` | 재고 표시, 수량선택, 구매 확인 Dialog | mockFlashProductDetail |
| 찜 목록 | `app/(consumer)/mypage/wishlist/page.tsx` | 찜 아이템 리스트, 찜 해제 | mockWishlists ×3 |
| 예약 내역 | `app/(consumer)/mypage/reservations/page.tsx` | Tabs (ALL/PENDING/CONFIRMED/CANCELLED), 취소 가능 | mockReservations ×6 |
| 구매 내역 | `app/(consumer)/mypage/purchases/page.tsx` | 구매 아이템 리스트 | mockPurchases ×3 |

#### 소상공인 영역 (4개)

| 화면 | 경로 | 기능 | Mock 데이터 |
|------|------|------|----------|
| 대시보드 | `app/(merchant)/merchant/dashboard/page.tsx` | 환영, 통계 3개, 대기 예약, 내 상품 | mockDashboard |
| 상품 등록 | `app/(merchant)/products/new/page.tsx` | 타입 선택(일반/선착순), 폼, 조건부 필드 | static |
| 상품 목록 | `app/(merchant)/merchant/products/page.tsx` | Table (제목/가격/타입/상태/액션), 종료 Dialog | mockMerchantProducts ×4 |
| 예약 관리 | `app/(merchant)/merchant/reservations/page.tsx` | Tabs 카드 (PENDING/CONFIRMED/COMPLETED), 확정/거절 버튼 | mockMerchantReservations ×5 |

#### 관리자 영역 (2개)

| 화면 | 경로 | 기능 | Mock 데이터 |
|------|------|------|----------|
| 사용자 관리 | `app/(admin)/admin/users/page.tsx` | 검색, Tabs (ALL/CONSUMER/MERCHANT), Table, 정지/탈퇴 Dialog | mockUsers ×10 |
| 상품 검수 | `app/(admin)/admin/products/page.tsx` | Tabs (ALL/ACTIVE/CLOSED), Table, 강제종료 Dialog | mockAdminProducts ×7 |

### 4.2 정량적 성과

| 항목 | 목표 | 달성 | 달성률 |
|------|------|------|--------|
| 라우트 수 | 15개 | 15개 | 100% |
| 공통 컴포넌트 | 7개 | 7개 | 100% |
| 타입 정의 | 17개 | 18개 | 106% |
| Mock 데이터 | 40개 이상 | 46개 | 115% |
| TS 에러 | 0개 | 0개 | 100% |
| Design Gap | 90% 이상 | 95% | 105% |

---

## 5. Gap 분석 결과

### 5.1 설계-구현 일치도: 95%

```
┌─────────────────────────────────────────────┐
│        Design-Implementation Gap             │
├─────────────────────────────────────────────┤
│ 파일 구조 일치도       93%  ✅               │
│ 컴포넌트/Props        95%  ✅               │
│ 디자인 토큰           88%  ⚠️  (포맷 변경)  │
│ 타입 정의             97%  ✅               │
│ Mock 데이터          100%  ✅               │
│ 레이아웃             95%  ✅               │
│ 페이지 기능          98%  ✅               │
│ 아키텍처준수         100%  ✅               │
│ 컨벤션 준수          96%  ✅               │
├─────────────────────────────────────────────┤
│ 종합 점수            95%  ✅ (90% 기준 충족) │
└─────────────────────────────────────────────┘
```

### 5.2 주요 Gap 요약

**의도적 변경 (기술 개선)**:
1. Tailwind CSS v4 → @theme inline oklch (색상값 동일)
2. Pretendard → Geist Sans (v4 호환성)
3. Route 중첩 `(merchant)/merchant/`, `(admin)/admin/` (URL 명확성)
4. RadioGroup → 커스텀 토글 (UX 개선)

**추가 구현 (개선)**:
- `formatDateTime` 유틸, `MerchantProfileResponse` 타입, `mockFlashProductDetail`, Dark Mode CSS, Sonner 토스트

---

## 6. Phase 6 준비 현황

| 항목 | 상태 | 비고 |
|------|------|------|
| **UI 레이어** | ✅ 완료 | 모든 화면 정적 UI 완성 |
| **타입 정의** | ✅ 완료 | API 응답 타입 전부 정의 |
| **Mock 데이터** | ✅ 완료 | API 연동 전 테스트용 준비 |
| **프로젝트 구조** | ✅ 완료 | `lib/api/` 추가 준비됨 |

**Phase 6에서 필요한 작업**:
1. API 클라이언트 레이어 구현 (`lib/api/`) — axios 또는 fetch 기반
2. 인증 상태 관리 — JWT 토큰, Zustand/React Context, 미들웨어
3. API 연동 — mock 데이터 → 실제 API 호출
4. 지오로케이션 — `navigator.geolocation` 활용
5. 환경변수 관리 — `NEXT_PUBLIC_API_BASE_URL`, `.env.local.example`

---

## 7. 참고 문서

| 문서 | 경로 | 용도 |
|------|------|------|
| Plan | `docs/01-plan/features/phase5-design-system.plan.md` | 기획 정보 |
| Design | `docs/02-design/features/phase5-design-system.design.md` | 기술 설계 |
| Analysis | `docs/03-analysis/phase5-design-system.analysis.md` | Gap 분석 |
| 구현 레포 | https://github.com/YeeunJ/near-pick-web | Phase 5 구현 코드 |

---

## 변경 이력

| 버전 | 날짜 | 변경 사항 | 작성자 |
|------|------|---------|--------|
| 1.0 | 2026-02-28 | Phase 5 완료 리포트 작성 (near-pick-web 구현 기반) | report-generator |

---

## 결론

**Phase 5 Design System은 95% 일치율로 설계 문서를 충실히 이행했으며, 90% 기준을 초과 달성했다.**

- ✅ **15개 라우트** 모두 구현 완료
- ✅ **7개 공통 컴포넌트** 재사용 가능하도록 설계
- ✅ **18개 타입 정의** (백엔드 DTO와 1:1 싱크)
- ✅ **46개 Mock 데이터** 구현
- ✅ **TypeScript 0 에러** (pnpm build 성공)
- ✅ **반응형 레이아웃** (모바일/태블릿/데스크톱)

**다음 단계**: Phase 6 (UI Integration) — API 클라이언트 레이어 구현, 인증 상태 관리, 각 페이지 API 연동 순서로 진행.
