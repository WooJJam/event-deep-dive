package co.kr.woojjam.event_deep_dive.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long DEFAULT_PROCESSABLE_AFTER_MINUTES = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OutboxEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    /**
     * Primary path(리스너)에게 부여하는 유예 시간.
     * 이 시각 이전에는 스케줄러가 해당 이벤트를 조회하지 않는다.
     * 정상 상황에서는 리스너가 처리 후 PROCESSED로 마킹하므로 스케줄러가 찾을 이벤트가 없다.
     */
    @Column(nullable = false)
    private LocalDateTime processableAfter;

    /**
     * 스케줄러 재시도 경로에서 Exponential Backoff 대기 시각.
     */
    private LocalDateTime nextRetryAt;

    private LocalDateTime processingStartedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Outbox(Long paymentId, OutboxEventType eventType, String payload,
                   OutboxStatus status, int retryCount,
                   LocalDateTime processableAfter, LocalDateTime nextRetryAt) {
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.processableAfter = processableAfter;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = LocalDateTime.now();
    }

    public static Outbox create(Long paymentId, OutboxEventType eventType, String payload) {
        return init(paymentId, eventType, payload, DEFAULT_PROCESSABLE_AFTER_MINUTES);
    }

    public static Outbox create(Long paymentId, OutboxEventType eventType, String payload, long processableAfterMinutes) {
        return init(paymentId, eventType, payload, processableAfterMinutes);
    }

    private static Outbox init(Long paymentId, OutboxEventType eventType, String payload, long processableAfterMinutes) {
        return Outbox.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .processableAfter(LocalDateTime.now().plusMinutes(processableAfterMinutes))
                .nextRetryAt(null)
                .build();
    }

    // ── 상태 전이 메서드 ──────────────────────────────────────────

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    public void markProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.nextRetryAt = null;
    }

    /**
     * 처리 실패 시 호출. retryCount를 증가시키고 Exponential Backoff 대기 시각을 설정한다.
     * MAX_RETRY_COUNT 초과 시 Dead Letter로 전환한다.
     */
    public void handleFailure() {
        this.retryCount++;
        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            this.nextRetryAt = calculateNextRetryAt();
        }
    }

    private LocalDateTime calculateNextRetryAt() {
        return switch (this.retryCount) {
            case 1 -> LocalDateTime.now().plusMinutes(1);
            case 2 -> LocalDateTime.now().plusMinutes(5);
            default -> LocalDateTime.now().plusMinutes(15);
        };
    }

    public boolean isDeadLetter() {
        return this.status == OutboxStatus.FAILED;
    }
}
