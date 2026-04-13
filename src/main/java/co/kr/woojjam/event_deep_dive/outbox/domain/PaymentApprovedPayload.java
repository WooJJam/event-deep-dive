package co.kr.woojjam.event_deep_dive.outbox.domain;

import java.math.BigDecimal;

public record PaymentApprovedPayload(
        Long paymentId,
        String userName,
        String userFcmToken,
        String stadiumOwnerPhone,
        String matchTitle,
        BigDecimal amount
) {
}
