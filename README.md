# Event Deep Dive

이벤트를 사용하며 발생하는 문제(이벤트 유실, 중복 처리)들을 직접 테스트하고 해결 패턴을 구현하는 프로젝트.

---

## 구현 단계 요약

### 1단계 — 문제 확인: Spring Event 기반 이벤트 유실 재현
Spring의 `ApplicationEventPublisher`로 이벤트를 발행한다.
앱 재시작 또는 예외 발생 시 이벤트가 유실되는 시나리오를 직접 재현한다.

### 2단계 — Outbox Pattern 구현 (Polling Publisher)
상태 업데이트와 Outbox INSERT를 동일 트랜잭션으로 묶는다.
스케줄러(ShedLock 포함)로 PENDING 이벤트를 폴링해 후속 처리한다.

### 3단계 — Redis Streams으로 Outbox 이벤트 처리 전환
DB 폴링 기반 Outbox를 Redis Streams(XADD / XREADGROUP / XACK) 기반으로 전환한다.
Consumer Group과 PEL을 활용해 "적어도 한 번" 처리를 보장하고 폴링 지연을 제거한다.
