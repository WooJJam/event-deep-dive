package co.kr.woojjam.event_deep_dive.payment.domain;

import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private String matchTitle;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String userFcmToken;

    @Column(nullable = false)
    private Long stadiumOwnerId;

    @Column(nullable = false)
    private String stadiumOwnerPhone;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(Long matchId, String matchTitle, Long userId, String userName, String userFcmToken,
                    Long stadiumOwnerId, String stadiumOwnerPhone, BigDecimal amount,
                    PaymentStatus status, String idempotencyKey) {
        this.matchId = matchId;
        this.matchTitle = matchTitle;
        this.userId = userId;
        this.userName = userName;
        this.userFcmToken = userFcmToken;
        this.stadiumOwnerId = stadiumOwnerId;
        this.stadiumOwnerPhone = stadiumOwnerPhone;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Payment from(PaymentRequest request) {
        return Payment.builder()
                .matchId(request.matchId())
                .matchTitle(request.matchTitle())
                .userId(request.userId())
                .userName(request.userName())
                .userFcmToken(request.userFcmToken())
                .stadiumOwnerId(request.stadiumOwnerId())
                .stadiumOwnerPhone(request.stadiumOwnerPhone())
                .amount(request.amount())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.idempotencyKey())
                .build();
    }

    // ── FSM 전이 메서드 ──────────────────────────────────────────

    public void approve() {
        validateStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        validateStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void requestCancel() {
        validateStatus(PaymentStatus.APPROVED);
        this.status = PaymentStatus.CANCEL_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void refund() {
        validateStatus(PaymentStatus.CANCEL_REQUESTED);
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("잘못된 결제 상태 전이입니다. 현재: %s, 필요: %s", this.status, expected)
            );
        }
    }
}
