# [Plan] phase9-performance — 고성능 아키텍처

## 메타데이터

| 항목 | 내용 |
|------|------|
| **Feature** | phase9-performance |
| **Phase** | 9 |
| **브랜치** | `feature/phase9-performance` |
| **시작일** | 2026-03-06 |
| **목표 TPS** | 평시 200 / 이벤트 3,000 / 선착순 10,000 |

---

## 1. 목표 (Why)

현재 NearPick 백엔드는 단일 서버 + 단일 DB 구조로 선착순 이벤트 트래픽(10,000 TPS)을 버티지 못한다.
Phase 9는 **고가용성 캐싱 레이어, 비동기 이벤트 처리, 동시성 제어**를 도입해 이후 모든 기능 확장의 기반 인프라를 구축하는 단계다.

---

## 2. 현재 상태 (As-Is)

| 항목 | 현재 |
|------|------|
| 재고 동시성 제어 | Pessimistic Lock (단일 DB) |
| Rate Limiting | ConcurrentHashMap (메모리, 인스턴스별) |
| 캐싱 | 없음 (매 요청 DB 조회) |
| 선착순 처리 | 단순 트랜잭션 (병목 위험) |
| DB | 단일 MySQL (읽기/쓰기 미분리) |

---

## 3. 요구사항 (What)

### P1 — 캐싱 레이어 (Redis)
- 상품 목록 / 상품 상세 캐시 (TTL: 30s~5min)
- 인기도 점수 캐시 (배치 갱신)
- 사용자 세션 / JWT 블랙리스트 (기존 메모리 → Redis)

### P2 — 선착순 구매 고성능화 (Kafka)
- 재고 감소 요청을 Kafka 토픽으로 발행
- Consumer가 순서 보장 처리 (파티션 키: product_id)
- 중복 요청 방지 (Idempotency Key)
- 재고 부족 시 Dead Letter Queue (DLQ) 처리

### P3 — 분산 Rate Limiting (Redis Bucket4j)
- 현재 ConcurrentHashMap → Redis 기반으로 교체
- 다중 인스턴스 환경에서 IP/User별 일관된 제한
- login/signup 별도 버킷 유지

### P4 — 동시성 제어 고도화 (Redisson)
- 현재 DB Pessimistic Lock → Redisson Distributed Lock
- FlashPurchase 선착순 처리에 적용
- Lock TTL 설정으로 데드락 방지

### P5 — Circuit Breaker (Resilience4j)
- 외부 의존성 장애 시 Fallback 처리
- 슬라이딩 윈도우 기반 실패율 모니터링

### P6 — DB Read Replica 준비 (추상화)
- `@Transactional(readOnly = true)` 전면 점검 및 적용
- DataSource 라우팅 설정 (로컬은 단일 DB, 추후 Replica 연결 대비)

---

## 4. 범위 (Scope)

### In Scope
- Redis 연동 (Spring Data Redis + Lettuce)
- Kafka 연동 (Spring Kafka)
- Redisson 분산 락
- Redis Bucket4j Rate Limiting
- Resilience4j Circuit Breaker
- readOnly 트랜잭션 정리
- **부하 테스트 (k6)**: 200 / 3,000 / 10,000 TPS 3개 시나리오 검증

### Out of Scope
- 실제 DB Read Replica 구성 (Phase 15 배포 시)
- Kubernetes / HPA (Phase 15)
- 캐시 Warm-up 자동화 (Phase 15)
- CI/CD 파이프라인 부하 테스트 자동화 (Phase 15)

---

## 5. 기술 결정 (How)

### Redis
- 클라이언트: Lettuce (Spring Boot 기본, 비동기 지원)
- 직렬화: Jackson JSON (타입 정보 포함)
- 로컬: Docker (`redis:7-alpine`)

### Kafka
- 클라이언트: Spring Kafka
- 토픽: `flash-purchase-requests`, `flash-purchase-dlq`
- 파티션: product_id 기준 (순서 보장)
- 로컬: Docker (`confluentinc/cp-kafka`)

### Redisson
- `redisson-spring-boot-starter`
- Lock TTL: 10s (재고 감소 처리 시간 기준)

### Circuit Breaker
- `resilience4j-spring-boot3` (Spring Boot 4.x 호환 확인 필요)
- 실패율 50% 초과 시 Open → Fallback 응답

### 부하 테스트
- 도구: **k6** (JavaScript 기반, 로컬 실행 간편, 결과 JSON 출력)
- 시나리오 파일 위치: `load-tests/` (프로젝트 루트)
- Phase 15에서 Gatling으로 CI/CD 통합 예정

---

## 6. 구현 순서

```
1. 인프라 설정 (docker-compose Redis + Kafka)
2. Redis 기본 연동 (Spring Data Redis)
3. 상품 캐싱 (ProductService 캐시 적용)
4. Redis Bucket4j Rate Limiting (RateLimitFilter 교체)
5. Redisson 분산 락 (FlashPurchaseService)
6. Kafka 연동 (선착순 구매 비동기 처리)
7. Circuit Breaker 적용
8. readOnly 트랜잭션 정리
9. 단위 테스트 보완
10. k6 부하 테스트 (200 / 3,000 / 10,000 TPS 시나리오)
11. 결과 분석 및 병목 수정
```

---

## 7. 성공 기준

| 항목 | 기준 |
|------|------|
| 단위 테스트 | 핵심 로직 커버 (Cache, Lock, Kafka Consumer) |
| 로컬 구동 | Redis + Kafka 포함 정상 기동 |
| 선착순 중복 방지 | 동시 100 요청 → 재고 초과 구매 없음 |
| **k6 평시 시나리오** | 200 VU / 1min — p95 응답 < 200ms, 에러율 < 1% |
| **k6 이벤트 시나리오** | 3,000 VU / 1min — p95 응답 < 500ms, 에러율 < 2% |
| **k6 선착순 시나리오** | 10,000 동시 요청 — 재고 초과 구매 0건, 429 외 에러율 < 1% |
| Match Rate | ≥ 90% |
