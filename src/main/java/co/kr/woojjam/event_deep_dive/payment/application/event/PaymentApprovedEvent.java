package co.kr.woojjam.event_deep_dive.payment.application.event;

import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentApprovedPayload;

public record PaymentApprovedEvent(
        Long outboxId,
        PaymentApprovedPayload payload
) {
}
