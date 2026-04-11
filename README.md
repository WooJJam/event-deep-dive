# Event-Driven Deep Dive

이벤트 기반 아키텍처에서 발생하는 신뢰성 문제(이벤트 유실, 중복 처리, 분산 트랜잭션)를 직접 경험하고 해결 패턴을 구현하는 학습 프로젝트.

---

## 학습 목표

이 레포를 완주하면 다음을 설명할 수 있게 된다:

- Spring Event 방식이 왜 이벤트 유실 위험을 가지는지, 그리고 Outbox 패턴이 어떻게 이를 해결하는지
- 분산 트랜잭션에서 2PC 없이 데이터 일관성을 보장하는 Saga 패턴의 작동 원리
- Webhook 수신 시 HMAC-SHA256 서명 검증과 멱등성 처리가 왜 함께 필요한지
- 재시도 설계에서 Exponential Backoff, 멱등성, Dead Letter Queue가 각각 어떤 역할을 하는지
- Redis Streams(Consumer Group, PEL, XACK)가 메시지 신뢰성을 어떻게 보장하는지
- 결제 상태 머신(FSM)이 잘못된 상태 전이를 방어하는 이유

---

## 기술 스택

- Java 21 (Virtual Thread) · Spring Boot 3.5
- MySQL · Spring Data JPA
- Redis Streams (XADD / XREADGROUP / XACK / Consumer Group)
- Toss Payments (Webhook 수신 및 서명 검증)
- ShedLock (분산 스케줄러 단일 실행)
- k6 (부하 테스트 및 재시도 시나리오 검증)

---

## 디렉토리 구조

```
event-driven-deep-dive/
├── docs/
│   ├── 요구사항명세서.md       # 기능 / 비기능 / 기술 요구사항
│   └── 학습계획.md             # 단계별 구현 순서 및 검증 방법
├── src/
│   └── main/java/
│       ├── payment/
│       │   ├── domain/         # 결제 상태 머신 (FSM)
│       │   ├── application/    # Facade, Service
│       │   ├── infrastructure/ # JPA Entity, Outbox Repository
│       │   └── presentation/   # Webhook Controller
│       ├── outbox/             # Outbox 스케줄러, Polling Publisher
│       ├── saga/               # Saga 흐름 및 보상 트랜잭션 (예정)
│       └── global/             # 공통 설정, 예외
└── README.md
```

---

## 구현 단계 요약

### 1단계 — 문제 확인: Spring Event 기반 이벤트 유실 재현
Spring의 `ApplicationEventPublisher`로 결제 완료 이벤트를 발행한다.
앱 재시작 또는 예외 발생 시 이벤트가 유실되는 시나리오를 직접 재현한다.

### 2단계 — Outbox Pattern 구현 (Polling Publisher)
결제 상태 업데이트와 Outbox INSERT를 동일 트랜잭션으로 묶는다.
스케줄러(ShedLock 포함)로 PENDING 이벤트를 폴링해 후속 처리한다.

### 3단계 — Saga / 보상 트랜잭션 구현 (Choreography)
매치 확정 실패 시 결제 환불이 자동으로 트리거되는 흐름을 구현한다.
각 단계를 독립된 트랜잭션으로 분리하고 보상 이벤트로 일관성을 복구한다.

### 4단계 — Webhook 멱등성 + 재시도 전략
HMAC-SHA256 서명 검증으로 위변조된 Webhook을 차단한다.
WebhookLog 테이블로 중복 수신을 방어하고, Exponential Backoff + Dead Letter Queue로 재시도 전략을 완성한다.

### 5단계 — Redis Streams으로 Outbox 이벤트 처리 전환
DB 폴링 기반 Outbox를 Redis Streams(XADD / XREADGROUP / XACK) 기반으로 전환한다.
Consumer Group과 PEL을 활용해 "적어도 한 번" 처리를 보장하고 폴링 지연을 제거한다.

---

## 면접 포인트 요약

| 질문 | 핵심 답변 포인트 |
|------|----------------|
| Outbox 패턴을 왜 도입했나요? | Spring Event는 앱 재시작 시 이벤트 유실 가능. 동일 트랜잭션에서 DB에 기록하므로 유실 불가 구조 |
| Saga Choreography를 선택한 이유는? | Orchestrator 추가가 단일 서버에서는 과도한 복잡도. 이벤트 기반으로 각 단계 독립 처리 |
| Webhook 멱등성을 어떻게 구현했나요? | WebhookLog 테이블에 eventId 기록. 중복 수신 시 DB 유니크 제약으로 차단 |
| Exponential Backoff를 선택한 이유는? | 고정 간격 재시도는 장애 상황에서 서버 부하를 증폭. 지수 백오프로 부하 분산 |
| Redis Streams의 PEL 역할은? | XACK 전까지 처리 중인 메시지를 추적. 소비자 장애 시 미처리 메시지 재할당 가능 |