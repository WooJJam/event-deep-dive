package co.kr.woojjam.event_deep_dive.payment.application.dto;

import co.kr.woojjam.event_deep_dive.payment.domain.Payment;
import co.kr.woojjam.event_deep_dive.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long matchId,
        String matchTitle,
        String userName,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMatchId(),
                payment.getMatchTitle(),
                payment.getUserName(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
