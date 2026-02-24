# NearPick

> 지역 기반 실시간 인기 상품 커머스 플랫폼

소비자는 근처 인기 상품을 탐색하고 찜·예약·선착순 구매를 할 수 있으며,
소상공인은 상품과 할인권을 등록해 노출·판매한다. 모든 거래는 **직접 방문 수령** 기반이다.

---

## Tech Stack

| 항목 | 기술 |
|------|------|
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.3 |
| JVM | Java 17 |
| ORM | Spring Data JPA (Hibernate) |
| Build | Gradle 9 (Kotlin DSL, multi-module) |
| DB (dev) | H2 in-memory (PostgreSQL 모드) |
| DB (prod) | PostgreSQL |

---

## Module Structure

```
near-pick/
├── app/             ← Controller, 진입점 (@SpringBootApplication)
├── common/          ← 공통 응답·예외·유틸 (독립 모듈)
├── domain/          ← Service 인터페이스, 순수 도메인 모델, DTO, Enum
└── domain-nearpick/ ← Service 구현체, JPA Entity, Repository, Mapper
```

### 의존성 흐름

```
app ──(compile)──────> domain ──(compile)──> common
 └──(runtimeOnly)──> domain-nearpick ──> domain, common
```

> `app`은 `domain-nearpick`을 `runtimeOnly`로만 의존합니다.
> Controller에서 Repository나 Entity를 직접 `import`하면 **컴파일 에러**가 발생합니다.

자세한 내용 → [모듈 구조 가이드](docs/wiki/02-module-structure.md)

---

## Getting Started

### 사전 요구사항

- JDK 17 이상
- (선택) Docker — PostgreSQL 운영 환경 실행 시

### 빌드

```bash
./gradlew build
```

### 실행

```bash
./gradlew :app:bootRun
```

애플리케이션 기동 후 H2 Console: <http://localhost:8080/h2-console>

- JDBC URL: `jdbc:h2:mem:nearpick`
- Username: `sa` / Password: (없음)

### 테스트

```bash
./gradlew test
```

---

## Documentation

| 문서 | 설명 |
|------|------|
| [프로젝트 개요](docs/wiki/00-overview.md) | 서비스 소개, 핵심 기능, 비즈니스 규칙 |
| [도메인 용어사전](docs/wiki/01-domain-glossary.md) | 도메인 개념 정의 |
| [모듈 구조 가이드](docs/wiki/02-module-structure.md) | 멀티모듈 설계 상세 |
| [개발 참여 가이드](docs/wiki/03-dev-guide.md) | 브랜치·커밋·PR 워크플로우 |
| [코딩 컨벤션](CONVENTIONS.md) | 네이밍·JPA·Spring 규칙 |

---

## License

Private repository — all rights reserved.
