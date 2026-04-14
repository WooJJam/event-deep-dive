package co.kr.woojjam.event_deep_dive.payment.application.event;

public record PaymentApprovedEvent(
        Long outboxId
) {
}
