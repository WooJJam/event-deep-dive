package co.kr.woojjam.event_deep_dive.outbox.scheduler;

import co.kr.woojjam.event_deep_dive.notification.FcmNotificationService;
import co.kr.woojjam.event_deep_dive.notification.SmsNotificationService;
import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxStatus;
import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentApprovedPayload;
import co.kr.woojjam.event_deep_dive.outbox.domain.Outbox;
import co.kr.woojjam.event_deep_dive.outbox.infrastructure.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository paymentOutboxRepository;
    private final SmsNotificationService smsNotificationService;
    private final FcmNotificationService fcmNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Fallback Path: 1분마다 PENDING 이벤트를 폴링하여 처리한다.
     *
     * 조회 조건:
     *   - status = PENDING
     *   - processableAfter <= now : 리스너 유예 시간(5분) 경과
     *   - nextRetryAt IS NULL OR nextRetryAt <= now : Exponential Backoff 대기 완료
     *
     * 정상 상황에서는 Primary Listener가 이미 PROCESSED로 마킹했으므로 조회 결과가 없다.
     * 서버 crash 또는 리스너 실패로 PENDING이 남은 경우에만 동작한다.
     *
     * fixedDelay: 이전 실행이 완료된 후 1분 뒤 실행 (fixedRate 사용 시 처리 지연에 따른 중복 실행 위험)
     */
    @Scheduled(fixedDelay = 300_00)
    @Transactional
    public void processPendingEvents() {
        List<Outbox> events = paymentOutboxRepository.findEventsReadyToProcess(
                OutboxStatus.PENDING, LocalDateTime.now()
        );

        if (events.isEmpty()) {
            log.info("[Scheduler] 미처리 Outbox 이벤트가 존재하지 않습니다.");
            return;
        }

        log.info("[Scheduler] 미처리 Outbox 이벤트 {}건 발견", events.size());

        for (Outbox outbox : events) {
            processEvent(outbox);
        }
    }

    private void processEvent(Outbox outbox) {
        log.info("[Scheduler] Outbox 이벤트 처리 시작 - outboxId: {}, retryCount: {}",
                outbox.getId(), outbox.getRetryCount());
        try {
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
            log.info("[Scheduler] Outbox 이벤트 처리 완료 - outboxId: {}", outbox.getId());

        } catch (Exception e) {
            outbox.handleFailure();

            if (outbox.isDeadLetter()) {
                log.error("[Scheduler] Dead Letter 전환 - outboxId: {}, paymentId: {}. 운영팀 확인 필요.",
                        outbox.getId(), outbox.getPaymentId(), e);
            } else {
                log.warn("[Scheduler] Outbox 이벤트 처리 실패 - outboxId: {}, retryCount: {}, nextRetryAt: {}",
                        outbox.getId(), outbox.getRetryCount(), outbox.getNextRetryAt(), e);
            }
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
