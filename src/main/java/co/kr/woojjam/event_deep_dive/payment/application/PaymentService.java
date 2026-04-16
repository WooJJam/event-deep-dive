package co.kr.woojjam.event_deep_dive.payment.application;

import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxEventType;
import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentApprovedPayload;
import co.kr.woojjam.event_deep_dive.outbox.domain.Outbox;
import co.kr.woojjam.event_deep_dive.outbox.infrastructure.OutboxRepository;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentRequest;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentResponse;
import co.kr.woojjam.event_deep_dive.payment.application.event.PaymentApprovedEvent;
import co.kr.woojjam.event_deep_dive.payment.domain.Payment;
import co.kr.woojjam.event_deep_dive.payment.infrastructure.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository paymentOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 결제 처리 메인 로직.
     *
     * 트랜잭션 내에서 다음 두 작업을 원자적으로 처리한다:
     *   1. Payment 상태 APPROVED 전이 및 저장
     *   2. Outbox INSERT (status=PENDING, processableAfter=now+5분)
     *
     * 트랜잭션 커밋 후 @TransactionalEventListener(AFTER_COMMIT)가 즉시 SMS/FCM을 처리한다(Primary Path).
     * 서버 crash 등으로 리스너가 실행되지 못한 경우 Outbox Scheduler가 5분 후 폴링으로 복구한다(Fallback Path).
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // 1. 결제 승인 처리 (외부 PG API 호출 모킹)
        executePgApproval();

        // 2. Payment 생성 → PENDING → APPROVED 전이
        Payment payment = Payment.from(request);
        payment.approve();
        paymentRepository.save(payment);
        log.info("[Payment] 결제 승인 완료 - paymentId: {}, 사용자: {}", payment.getId(), payment.getUserName());

        // 3. Outbox INSERT (동일 트랜잭션 → Payment 저장과 원자적 처리)
        PaymentApprovedPayload payload = new PaymentApprovedPayload(
                payment.getId(),
                payment.getUserName(),
                payment.getUserFcmToken(),
                payment.getStadiumOwnerPhone(),
                payment.getMatchTitle(),
                payment.getAmount()
        );
        String payloadJson = serializePayload(payload);
        Outbox outbox = Outbox.create(payment.getId(), OutboxEventType.PAYMENT_APPROVED, payloadJson);
        paymentOutboxRepository.save(outbox);
        log.info("[Outbox] 이벤트 저장 완료 - outboxId: {}, paymentId: {}", outbox.getId(), payment.getId());

        // 4. Spring Event 발행 → AFTER_COMMIT 리스너 트리거 (Primary Path)
        // outboxId만 전달한다. 실제 payload는 리스너/스케줄러가 Outbox 테이블에서 직접 읽는다.
        eventPublisher.publishEvent(new PaymentApprovedEvent(outbox.getId()));

        return PaymentResponse.from(payment);
    }

    /**
     * PG사 결제 승인 API 호출을 Thread.sleep으로 모킹한다.
     */
    private void executePgApproval() {
        log.info("[Payment] PG사 결제 승인 API 호출 중...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PG 결제 승인 처리 중단", e);
        }
        log.info("[Payment] PG사 결제 승인 완료");
    }

    private String serializePayload(PaymentApprovedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox payload 직렬화 실패", e);
        }
    }
}
