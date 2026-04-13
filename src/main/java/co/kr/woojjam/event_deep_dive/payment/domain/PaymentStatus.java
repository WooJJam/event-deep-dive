package co.kr.woojjam.event_deep_dive.payment.domain;

public enum PaymentStatus {
    PENDING,
    APPROVED,
    FAILED,
    CANCEL_REQUESTED,
    REFUNDED
}
