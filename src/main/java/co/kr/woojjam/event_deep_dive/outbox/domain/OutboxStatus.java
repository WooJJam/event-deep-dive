package co.kr.woojjam.event_deep_dive.outbox.domain;

public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
