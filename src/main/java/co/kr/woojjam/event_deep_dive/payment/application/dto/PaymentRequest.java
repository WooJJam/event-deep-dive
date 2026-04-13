package co.kr.woojjam.event_deep_dive.payment.application.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        Long matchId,
        String matchTitle,
        Long userId,
        String userName,
        String userFcmToken,
        Long stadiumOwnerId,
        String stadiumOwnerPhone,
        BigDecimal amount,
        String idempotencyKey
) {
}
