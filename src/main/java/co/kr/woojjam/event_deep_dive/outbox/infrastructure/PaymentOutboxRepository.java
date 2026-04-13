package co.kr.woojjam.event_deep_dive.outbox.infrastructure;

import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxStatus;
import co.kr.woojjam.event_deep_dive.outbox.domain.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    /**
     * 스케줄러 폴링 쿼리.
     * - status = PENDING
     * - processableAfter <= now : 리스너 유예 시간이 지난 이벤트
     * - nextRetryAt IS NULL OR nextRetryAt <= now : 재시도 대기 시간이 지난 이벤트
     */
    @Query("""
            SELECT o FROM PaymentOutbox o
            WHERE o.status = :status
              AND o.processableAfter <= :now
              AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
            """)
    List<PaymentOutbox> findEventsReadyToProcess(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now
    );
}
