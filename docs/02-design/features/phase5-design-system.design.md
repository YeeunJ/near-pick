# [Design] Phase 5 — Design System (Next.js + shadcn/ui)

## 메타데이터

| 항목 | 내용 |
|------|------|
| Feature | phase5-design-system |
| Phase | Design |
| 작성일 | 2026-02-28 |
| 참조 | `docs/01-plan/features/phase5-design-system.plan.md`, `docs/02-design/features/phase3-mockup.design.md` |

---

## 1. 프로젝트 초기화

### 1-1. Next.js 프로젝트 생성

```bash
# 프로젝트 루트(near-pick/)에서
pnpm create next-app@latest web \
  --typescript \
  --tailwind \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --no-eslint
```

> `--src-dir` 미사용 — App Router는 `app/`을 최상위에 두는 것이 표준.
> `--no-eslint` — 프로젝트 루트 `.eslintrc` 충돌 방지, 추후 별도 설정.

### 1-2. 추가 패키지 설치

```bash
cd web
pnpm add lucide-react clsx tailwind-merge
pnpm add -D @types/node
```

### 1-3. shadcn/ui 초기화

```bash
pnpm dlx shadcn@latest init
```

초기화 옵션:
- Style: `Default`
- Base color: `Neutral`
- CSS variables: `Yes`

설치할 컴포넌트:
```bash
pnpm dlx shadcn@latest add button card input label badge dialog
pnpm dlx shadcn@latest add table tabs select textarea separator
pnpm dlx shadcn@latest add toast dropdown-menu avatar skeleton
```

---

## 2. 디자인 토큰

### 2-1. 색상 팔레트

**`web/app/globals.css`**

```css
@layer base {
  :root {
    /* NearPick Brand */
    --color-primary:       26 140 90;    /* #1A8C5A — 로컬 그린 (CTA) */
    --color-primary-light: 236 249 243;  /* #ECF9F3 — 연한 그린 배경 */
    --color-accent:        255 107 53;   /* #FF6B35 — 오렌지 (FLASH 배지) */

    /* Neutral */
    --color-surface:       248 247 244;  /* #F8F7F4 — 앱 배경 */
    --color-card:          255 255 255;  /* #FFFFFF — 카드 배경 */
    --color-border:        229 231 235;  /* #E5E7EB */

    /* Text */
    --color-text-primary:  26 26 26;     /* #1A1A1A */
    --color-text-secondary:107 114 128;  /* #6B7280 */
    --color-text-disabled: 209 213 219;  /* #D1D5DB */

    /* Status */
    --color-success:       34 197 94;
    --color-warning:       234 179 8;
    --color-error:         239 68 68;

    /* shadcn/ui 오버라이드 */
    --background:          248 247 244;
    --foreground:          26 26 26;
    --primary:             26 140 90;
    --primary-foreground:  255 255 255;
    --radius:              0.75rem;
  }
}
```

### 2-2. Tailwind 설정

**`web/tailwind.config.ts`**

```typescript
import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: ['./app/**/*.{ts,tsx}', './components/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: 'rgb(var(--color-primary) / <alpha-value>)',
        'primary-light': 'rgb(var(--color-primary-light) / <alpha-value>)',
        accent: 'rgb(var(--color-accent) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['Pretendard', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        DEFAULT: 'var(--radius)',
      },
    },
  },
  plugins: [],
}

export default config
```

### 2-3. Pretendard 폰트

**`web/app/layout.tsx`** (루트 레이아웃)

```tsx
import localFont from 'next/font/local'

const pretendard = localFont({
  src: '../public/fonts/PretendardVariable.woff2',
  variable: '--font-pretendard',
  display: 'swap',
})
```

> `public/fonts/PretendardVariable.woff2` — [GitHub](https://github.com/orioncactus/pretendard) 에서 다운로드.

---

## 3. TypeScript 타입 정의

**`web/types/api.ts`** — 백엔드 DTO와 1:1 싱크

```typescript
// ─── 공통 ────────────────────────────────────────
export type UserRole = 'CONSUMER' | 'MERCHANT' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN'
export type ProductType = 'GENERAL' | 'FLASH_SALE'
export type ProductStatus = 'ACTIVE' | 'CLOSED' | 'FORCE_CLOSED'
export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'
export type FlashPurchaseStatus = 'COMPLETED' | 'CANCELLED'

// ─── Auth ────────────────────────────────────────
export interface LoginResult {
  accessToken: string
  userId: number
  role: UserRole
}

export interface SignupResponse {
  userId: number
  email: string
  role: UserRole
}

// ─── Product ─────────────────────────────────────
export interface ProductSummaryResponse {
  id: number
  title: string
  price: number
  productType: ProductType
  status: ProductStatus
  shopAddress: string
  shopLat: number
  shopLng: number
  popularityScore: number
  remainingQuantity?: number    // FLASH_SALE only
  distanceKm?: number           // nearby 조회 시
}

export interface ProductDetailResponse extends ProductSummaryResponse {
  description: string
  merchantId: number
  wishlistCount: number
  reservationCount: number
  purchaseCount: number
  endsAt?: string               // FLASH_SALE only
}

// ─── Wishlist ─────────────────────────────────────
export interface WishlistItem {
  productId: number
  title: string
  price: number
  shopAddress: string
  productType: ProductType
  status: ProductStatus
}

// ─── Reservation ──────────────────────────────────
export interface ReservationItem {
  id: number
  productId: number
  productTitle: string
  quantity: number
  visitAt: string
  memo?: string
  status: ReservationStatus
}

export interface ReservationStatusResponse {
  reservationId: number
  status: ReservationStatus
}

// ─── FlashPurchase ────────────────────────────────
export interface FlashPurchaseItem {
  id: number
  productId: number
  productTitle: string
  quantity: number
  purchasedAt: string
  status: FlashPurchaseStatus
}

// ─── Merchant ─────────────────────────────────────
export interface MerchantDashboardResponse {
  shopName: string
  pendingReservationCount: number
  todayPurchaseCount: number
  totalPopularityScore: number
  recentReservations: ReservationItem[]
  myProducts: ProductSummaryResponse[]
}

// ─── Admin ────────────────────────────────────────
export interface UserSummary {
  userId: number
  email: string
  role: UserRole
  status: UserStatus
  createdAt: string
}

// ─── Pagination ───────────────────────────────────
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
```

---

## 4. 유틸리티

**`web/lib/utils.ts`**

```typescript
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatPrice(price: number): string {
  return `₩${price.toLocaleString('ko-KR')}`
}

export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  })
}
```

---

## 5. Mock 데이터

**`web/lib/mock/products.ts`**

```typescript
import type { ProductSummaryResponse, ProductDetailResponse } from '@/types/api'

export const mockProducts: ProductSummaryResponse[] = [
  {
    id: 1, title: '아메리카노 (당일 할인)', price: 2500,
    productType: 'GENERAL', status: 'ACTIVE',
    shopAddress: '서울 강남구 역삼동 123', shopLat: 37.500, shopLng: 127.036,
    popularityScore: 127, distanceKm: 0.3,
  },
  {
    id: 2, title: '딸기 케이크 선착순', price: 5900,
    productType: 'FLASH_SALE', status: 'ACTIVE',
    shopAddress: '서울 강남구 삼성동 45', shopLat: 37.508, shopLng: 127.062,
    popularityScore: 89, remainingQuantity: 8, distanceKm: 0.7,
  },
  // ... 3~5개 추가
]

export const mockProductDetail: ProductDetailResponse = {
  ...mockProducts[0],
  description: '당일 원두로 내린 신선한 아메리카노. 오후 2시까지 주문 시 10% 할인.',
  merchantId: 1,
  wishlistCount: 12, reservationCount: 5, purchaseCount: 3,
}
```

**`web/lib/mock/reservations.ts`**, **`web/lib/mock/users.ts`** — 동일 패턴으로 작성

---

## 6. 공통 레이아웃 컴포넌트

### 6-1. 루트 레이아웃

**`web/app/layout.tsx`**

```tsx
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className={`${pretendard.variable} font-sans bg-surface text-text-primary`}>
        {children}
      </body>
    </html>
  )
}
```

### 6-2. 소비자 레이아웃 (`(consumer)/layout.tsx`)

```tsx
// 상단 Header + 하단 BottomNav
export default function ConsumerLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col max-w-[430px] mx-auto">
      <ConsumerHeader />
      <main className="flex-1 pb-16">{children}</main>
      <BottomNav />
    </div>
  )
}
```

**`components/layout/ConsumerHeader.tsx`**
```tsx
interface ConsumerHeaderProps {
  location?: string   // "강남구 역삼동"
  showBack?: boolean
  title?: string
}
```

**`components/layout/BottomNav.tsx`**
```
탭: 홈(Home) | 찜(Heart) | 예약(Calendar) | 마이(User)
```

### 6-3. 소상공인/관리자 레이아웃 (`(merchant)/layout.tsx`, `(admin)/layout.tsx`)

```tsx
// 좌측 Sidebar + 우측 콘텐츠
export default function MerchantLayout({ children }) {
  return (
    <div className="flex min-h-screen">
      <MerchantSidebar />
      <main className="flex-1 p-6">{children}</main>
    </div>
  )
}
```

**`components/layout/MerchantSidebar.tsx`**
```
메뉴: 대시보드 | 상품 관리 | 예약 관리 | 로그아웃
```

---

## 7. 공통 UI 컴포넌트

### 7-1. ProductCard (`components/features/product/ProductCard.tsx`)

```tsx
interface ProductCardProps {
  product: ProductSummaryResponse
  onClick?: () => void
}
```

```
┌────────────────────┐
│  [이미지 placeholder]│
│  상품명              │
│  ₩ 2,500            │
│  📍 0.3km           │
│  ⭐ 127    [FLASH]  │
└────────────────────┘
```

### 7-2. StatusBadge (`components/features/StatusBadge.tsx`)

```tsx
interface StatusBadgeProps {
  status: ProductStatus | ReservationStatus | UserStatus
}
// ACTIVE→green, FLASH_SALE→orange, PENDING→yellow, CONFIRMED→blue,
// CANCELLED→gray, SUSPENDED→red
```

### 7-3. EmptyState (`components/ui/EmptyState.tsx`)

```tsx
interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
  action?: { label: string; href: string }
}
```

### 7-4. PageHeader (`components/layout/PageHeader.tsx`)

```tsx
interface PageHeaderProps {
  title: string
  showBack?: boolean
  action?: React.ReactNode   // 우측 버튼
}
```

---

## 8. 화면별 컴포넌트 설계

### AUTH-01: 로그인 (`app/(auth)/login/page.tsx`)

**컴포넌트**:
- `Input` (이메일, 비밀번호)
- `Button` ("로그인") — `variant="default"`, `className="w-full bg-primary"`
- `Link` ("회원가입 →")

**레이아웃**: 중앙 정렬 카드, `max-w-sm`

---

### AUTH-02: 회원가입 (`app/(auth)/signup/page.tsx`)

**컴포넌트**:
- `RadioGroup` (소비자/소상공인 선택)
- 소상공인 조건부 섹션: `Input` (가게명, 사업자번호, 주소)
- `Button` ("가입하기")

**조건부 렌더**: `role === 'MERCHANT'`이면 추가 필드 표시

---

### CON-01: 홈 (`app/(consumer)/page.tsx`)

**컴포넌트**:
- `ConsumerHeader` (위치 표시)
- 필터 바: `Select` (반경), `Tabs` (인기순/거리순)
- `ProductCard` 그리드 (2열, `gap-3`)
- `Button` ("더 보기") — Mock 페이지네이션

**Mock 데이터**: `mockProducts` (5개)

---

### CON-02: 상품 상세 (`app/(consumer)/products/[id]/page.tsx`)

**컴포넌트**:
- `PageHeader` (뒤로 가기)
- 이미지 placeholder (`aspect-video`, `bg-gray-100`)
- 상품 정보 섹션: 제목, 가격, 주소, 거리
- 인기 지표: 찜 수, 예약 수, 구매 수 (`LucideIcon` + 숫자)
- 재고 뱃지 (FLASH_SALE인 경우): `Badge variant="destructive"` ("남은 수량: N개")
- 액션 버튼 영역:
  - 찜 버튼: `Button variant="outline"` + `Heart` 아이콘
  - 예약하기: `Button variant="outline"`
  - 선착순 구매: `Button` (FLASH_SALE 타입일 때만 표시)

---

### CON-03: 예약하기 (`app/(consumer)/products/[id]/reserve/page.tsx`)

**컴포넌트**:
- `PageHeader` ("예약하기")
- 상품 요약 (제목, 가격)
- `Input type="date"` (방문 날짜)
- `Input type="time"` (방문 시간)
- `Select` (수량 1~10)
- `Textarea` (요청 사항)
- 총 금액 계산 표시 (가격 × 수량)
- `Button` ("예약 확정") — `className="w-full"`

---

### CON-04: 선착순 구매 (`app/(consumer)/products/[id]/purchase/page.tsx`)

**컴포넌트**:
- `PageHeader` ("선착순 구매")
- 재고 강조 표시: `Badge` ("남은 수량: N개")
- `Select` (수량)
- 총 금액 표시
- 경고 문구: `⚠️ 구매 후 취소 불가`
- `Dialog` 확인: "지금 구매하시겠습니까?" (취소/구매 버튼)

---

### CON-05: 마이페이지 탭 (`app/(consumer)/mypage/`)

**`wishlist/page.tsx`**:
- `PageHeader` ("찜 목록")
- 아이템 리스트: 이미지 placeholder + 상품명 + 가격 + 주소
- 찜 해제 버튼 (`Trash2` 아이콘)
- 비어있을 때 `EmptyState`

**`reservations/page.tsx`**:
- `Tabs` (대기중/확정/취소)
- `ReservationItem` 카드: 상품명, 방문일시, 수량, `StatusBadge`
- PENDING 상태일 때만 "취소" 버튼

**`purchases/page.tsx`**:
- 구매 아이템 리스트: 상품명, 구매일시, 수량

---

### MER-01: 대시보드 (`app/(merchant)/dashboard/page.tsx`)

**컴포넌트**:
- 환영 메시지 ("안녕하세요, {shopName}!")
- 통계 카드 3개 (`Card`):
  - 대기 예약 수 (`Calendar`)
  - 오늘 구매 수 (`ShoppingBag`)
  - 총 인기점수 (`TrendingUp`)
- 대기 예약 섹션: 예약자명, 상품명, 방문일시
- 내 상품 섹션: 상품명, 가격, `StatusBadge`

**Mock 데이터**: `mockDashboard`

---

### MER-02: 상품 등록 (`app/(merchant)/products/new/page.tsx`)

**컴포넌트**:
- `RadioGroup` (일반/선착순)
- `Input` (상품명, 가격)
- `Textarea` (상품 설명)
- FLASH_SALE 조건부:
  - `Input type="number"` (수량)
  - `Input type="datetime-local"` (종료일시)
- `Button` ("등록하기")

---

### MER-03: 상품 목록 (`app/(merchant)/products/page.tsx`)

**컴포넌트**:
- `PageHeader` ("내 상품", action=`Button` "+ 등록")
- `Table`: 상품명 / 가격 / 타입 / `StatusBadge` / 액션
- 액션 셀: "종료" `Button variant="destructive" size="sm"`
- `Dialog` 종료 확인

---

### MER-04: 예약 관리 (`app/(merchant)/reservations/page.tsx`)

**컴포넌트**:
- `Tabs` (대기중/확정/완료) + 각 탭에 건수 뱃지
- 예약 카드: 소비자명, 상품명, 수량, 방문일시
- PENDING일 때: "확정" `Button` + "거절" `Button variant="outline"`

---

### ADM-01: 사용자 관리 (`app/(admin)/users/page.tsx`)

**컴포넌트**:
- `Input` 검색 (이메일)
- `Tabs` (전체/소비자/소상공인)
- `Table`: 이메일 / 역할 / 상태 / 가입일 / 액션
- 액션: "정지" / "탈퇴" (각각 `Dialog` 확인)

---

### ADM-02: 상품 검수 (`app/(admin)/products/page.tsx`)

**컴포넌트**:
- `Tabs` (전체/ACTIVE/CLOSED)
- `Table`: 상품명 / 가게명 / 가격 / `StatusBadge` / 강제종료 버튼
- "강제종료" — `Button variant="destructive" size="sm"` + `Dialog` 확인

---

## 9. 구현 순서

```
[Step 1: 프로젝트 셋업]
1. pnpm create next-app web + shadcn/ui init
2. 패키지 추가 (lucide-react, clsx, tailwind-merge)
3. shadcn 컴포넌트 설치 (button, card, input, badge, dialog, table, tabs, select, textarea, toast)
4. globals.css — 디자인 토큰 (CSS variables)
5. tailwind.config.ts — 색상/폰트 확장
6. Pretendard 폰트 추가 + layout.tsx 루트 설정
7. lib/utils.ts (cn, formatPrice, formatDate)
8. types/api.ts — 전체 타입 정의

[Step 2: 공통 컴포넌트]
9.  components/layout/ConsumerHeader.tsx
10. components/layout/BottomNav.tsx
11. components/layout/MerchantSidebar.tsx
12. components/layout/PageHeader.tsx
13. components/ui/EmptyState.tsx
14. components/features/StatusBadge.tsx
15. components/features/product/ProductCard.tsx

[Step 3: Mock 데이터]
16. lib/mock/products.ts
17. lib/mock/reservations.ts
18. lib/mock/users.ts

[Step 4: 레이아웃 라우트]
19. app/(auth)/layout.tsx
20. app/(consumer)/layout.tsx
21. app/(merchant)/layout.tsx
22. app/(admin)/layout.tsx

[Step 5: AUTH 화면]
23. app/(auth)/login/page.tsx
24. app/(auth)/signup/page.tsx

[Step 6: 소비자 화면]
25. app/(consumer)/page.tsx          — CON-01: 홈
26. app/(consumer)/products/[id]/page.tsx       — CON-02: 상품 상세
27. app/(consumer)/products/[id]/reserve/page.tsx   — CON-03: 예약
28. app/(consumer)/products/[id]/purchase/page.tsx  — CON-04: 선착순 구매
29. app/(consumer)/mypage/wishlist/page.tsx     — CON-05a
30. app/(consumer)/mypage/reservations/page.tsx — CON-05b
31. app/(consumer)/mypage/purchases/page.tsx    — CON-05c

[Step 7: 소상공인 화면]
32. app/(merchant)/dashboard/page.tsx
33. app/(merchant)/products/new/page.tsx
34. app/(merchant)/products/page.tsx
35. app/(merchant)/reservations/page.tsx

[Step 8: 관리자 화면]
36. app/(admin)/users/page.tsx
37. app/(admin)/products/page.tsx

[Step 9: 검증]
38. pnpm dev — 전체 라우트 접근 확인
39. 모바일(375px) / 태블릿(768px) / 데스크톱(1280px) 반응형 확인
40. pnpm build — TypeScript 에러 없음 확인
```

---

## 10. 파일 구조 (전체)

```
web/
├── app/
│   ├── layout.tsx                       # 루트 레이아웃 (Pretendard, body)
│   ├── (auth)/
│   │   ├── layout.tsx
│   │   ├── login/page.tsx               # AUTH-01
│   │   └── signup/page.tsx              # AUTH-02
│   ├── (consumer)/
│   │   ├── layout.tsx                   # ConsumerHeader + BottomNav
│   │   ├── page.tsx                     # CON-01: 홈
│   │   ├── products/
│   │   │   └── [id]/
│   │   │       ├── page.tsx             # CON-02: 상품 상세
│   │   │       ├── reserve/page.tsx     # CON-03: 예약
│   │   │       └── purchase/page.tsx    # CON-04: 선착순 구매
│   │   └── mypage/
│   │       ├── wishlist/page.tsx        # CON-05a
│   │       ├── reservations/page.tsx    # CON-05b
│   │       └── purchases/page.tsx       # CON-05c
│   ├── (merchant)/
│   │   ├── layout.tsx                   # MerchantSidebar
│   │   ├── dashboard/page.tsx           # MER-01
│   │   ├── products/
│   │   │   ├── page.tsx                 # MER-03: 상품 목록
│   │   │   └── new/page.tsx             # MER-02: 상품 등록
│   │   └── reservations/page.tsx        # MER-04
│   └── (admin)/
│       ├── layout.tsx                   # AdminSidebar
│       ├── users/page.tsx               # ADM-01
│       └── products/page.tsx            # ADM-02
├── components/
│   ├── ui/                              # shadcn/ui 자동 생성
│   │   └── EmptyState.tsx              # 커스텀 추가
│   ├── layout/
│   │   ├── ConsumerHeader.tsx
│   │   ├── BottomNav.tsx
│   │   ├── MerchantSidebar.tsx
│   │   ├── AdminSidebar.tsx
│   │   └── PageHeader.tsx
│   └── features/
│       ├── StatusBadge.tsx
│       └── product/
│           └── ProductCard.tsx
├── lib/
│   ├── utils.ts
│   └── mock/
│       ├── products.ts
│       ├── reservations.ts
│       └── users.ts
├── types/
│   └── api.ts
└── public/
    └── fonts/
        └── PretendardVariable.woff2
```

---

## 11. 리스크 및 대응

| 리스크 | 대응 |
|--------|------|
| Tailwind v4 + shadcn/ui 호환 이슈 | 초기화 단계에서 확인; 문제 시 Tailwind v3 고정 |
| Pretendard 폰트 번들 크기 | `subset` 빌드 또는 Variable 폰트 단일 파일 사용 |
| `[id]` 동적 라우트 정적 렌더 | `generateStaticParams` 불필요 — Phase 5는 dev 모드만 확인 |
| BottomNav active 상태 | `usePathname()` 사용 (`"use client"` 필요) |
| 소비자/소상공인 레이아웃 최대 너비 | 소비자: `max-w-[430px] mx-auto` (모바일 우선), 소상공인/관리자: 풀 너비 |
