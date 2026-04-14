package co.kr.woojjam.event_deep_dive.payment.application;

import co.kr.woojjam.event_deep_dive.notification.FcmNotificationService;
import co.kr.woojjam.event_deep_dive.notification.SmsNotificationService;
import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentApprovedPayload;
import co.kr.woojjam.event_deep_dive.outbox.domain.Outbox;
import co.kr.woojjam.event_deep_dive.outbox.infrastructure.OutboxRepository;
import co.kr.woojjam.event_deep_dive.payment.application.event.PaymentApprovedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OutboxRepository paymentOutboxRepository;
    private final SmsNotificationService smsNotificationService;
    private final FcmNotificationService fcmNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Primary Path: 결제 트랜잭션 커밋 직후 실행.
     *
     * REQUIRES_NEW로 별도 트랜잭션을 열어 Outbox를 PROCESSED로 마킹한다.
     * SMS 또는 FCM 전송 실패 시 예외가 전파되어 이 트랜잭션이 롤백되므로
     * Outbox는 PENDING 상태를 유지하고, Fallback Scheduler가 복구를 담당한다.
     *
     * 서버 crash 시에는 이 메서드 자체가 실행되지 않으므로
     * Outbox는 PENDING 상태로 남아 Scheduler가 processableAfter 이후 처리한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentApproved(final PaymentApprovedEvent event) {
        log.info("[Listener] PAYMENT_APPROVED 이벤트 수신 - outboxId: {}", event.outboxId());

        Outbox outbox = paymentOutboxRepository.findById(event.outboxId())
                .orElseThrow(() -> new IllegalStateException("Outbox 이벤트를 찾을 수 없습니다. id: " + event.outboxId()));

        try {
            // 이벤트 객체가 아닌 Outbox 테이블의 payload를 단일 진실 공급원으로 사용한다.
            PaymentApprovedPayload payload = deserializePayload(outbox.getPayload());

            smsNotificationService.sendToStadiumOwner(
                    payload.stadiumOwnerPhone(),
                    payload.userName(),
                    payload.matchTitle(),
                    payload.amount()
            );

            fcmNotificationService.sendToUser(
                    payload.userFcmToken(),
                    payload.userName(),
                    payload.matchTitle()
            );

            outbox.markProcessed();
            log.info("[Listener] 이벤트 처리 완료 - outboxId: {}", event.outboxId());

        } catch (Exception e) {
            // 처리 실패 시 Outbox는 PENDING 상태를 유지한다.
            // Outbox Scheduler가 processableAfter(5분) 이후 Fallback으로 재처리한다.
            log.error("[Listener] Primary path 처리 실패 - outboxId: {}. Scheduler Fallback에 위임합니다.",
                    event.outboxId(), e);
        }
    }

    private PaymentApprovedPayload deserializePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, PaymentApprovedPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox payload 역직렬화 실패", e);
        }
    }
}
