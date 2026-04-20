package co.kr.woojjam.event_deep_dive.outbox.scheduler.worker;

import co.kr.woojjam.event_deep_dive.notification.FcmNotificationService;
import co.kr.woojjam.event_deep_dive.notification.SmsNotificationService;
import co.kr.woojjam.event_deep_dive.outbox.domain.Outbox;
import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxStatus;
import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentApprovedPayload;
import co.kr.woojjam.event_deep_dive.outbox.infrastructure.OutboxJdbcRepository;
import co.kr.woojjam.event_deep_dive.outbox.infrastructure.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.type", havingValue = "skip-locked")
public class SkipLockedOutboxWorker {

    private final OutboxRepository outboxRepository;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final SmsNotificationService smsNotificationService;
    private final FcmNotificationService fcmNotificationService;
    private final ObjectMapper objectMapper;

    public void process() {
        LocalDateTime now = LocalDateTime.now();

        // Step 0: crash 복구 - 5분 초과 PROCESSING → PENDING
        int recovered = outboxJdbcRepository.resetStaleProcessing(now.minusMinutes(5));
        if (recovered > 0) {
            log.warn("[SkipLocked] stale PROCESSING 복구: {}건", recovered);
        }

        // Step 1: 처리 대상 수 계산 및 batch claim
        int pendingCount = outboxJdbcRepository.countPending(now);
        if (pendingCount == 0) {
            log.info("[SkipLocked] 미처리 Outbox 이벤트가 존재하지 않습니다.");
            return;
        }

        int activeInstances = outboxJdbcRepository.countActiveInstances();
        int limit = (pendingCount / activeInstances) + 10;

        List<Long> ids = outboxJdbcRepository.claimBatch(limit, now);
        if (ids.isEmpty()) {
            log.info("[SkipLocked] 선점 가능한 Outbox 이벤트가 없습니다.");
            return;
        }

        log.info("[SkipLocked] {}건 claim (pendingCount={}, activeInstances={}, limit={})",
                ids.size(), pendingCount, activeInstances, limit);

        // Step 2: record별 즉시 개별 commit
        for (Long id : ids) {
            processOne(id);
        }
    }

    private void processOne(Long id) {
        Outbox outbox = outboxRepository.findById(id).orElse(null);
        if (outbox == null) {
            log.warn("[SkipLocked] Outbox 조회 실패 - outboxId: {}", id);
            return;
        }

        log.info("[SkipLocked] 처리 시작 - outboxId: {}, retryCount: {}", id, outbox.getRetryCount());

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

            outboxJdbcRepository.markProcessed(id);
            log.info("[SkipLocked] 처리 완료 - outboxId: {}", id);

        } catch (Exception e) {
            int newRetryCount = outbox.getRetryCount() + 1;

            if (newRetryCount >= 3) {
                outboxJdbcRepository.markFailed(id, newRetryCount, null, OutboxStatus.FAILED);
                log.error("[SkipLocked] Dead Letter 전환 - outboxId: {}, paymentId: {}", id, outbox.getPaymentId(), e);
            } else {
                LocalDateTime nextRetryAt = calculateNextRetryAt(newRetryCount);
                outboxJdbcRepository.markFailed(id, newRetryCount, nextRetryAt, OutboxStatus.PENDING);
                log.warn("[SkipLocked] 처리 실패 - outboxId: {}, retryCount: {}, nextRetryAt: {}", id, newRetryCount, nextRetryAt, e);
            }
        }
    }

    private LocalDateTime calculateNextRetryAt(int retryCount) {
        return switch (retryCount) {
            case 1 -> LocalDateTime.now().plusMinutes(1);
            case 2 -> LocalDateTime.now().plusMinutes(5);
            default -> LocalDateTime.now().plusMinutes(15);
        };
    }

    private PaymentApprovedPayload deserializePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, PaymentApprovedPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox payload 역직렬화 실패", e);
        }
    }
}
