package co.kr.woojjam.event_deep_dive.outbox.infrastructure;

import co.kr.woojjam.event_deep_dive.outbox.domain.Outbox;
import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Query("""
            SELECT o FROM Outbox o
            WHERE o.status = :status
              AND o.processableAfter <= :now
              AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
            """)
    List<Outbox> findPendingOutbox(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "org.hibernate.lockMode.o", value = "UPGRADE_SKIPLOCKED"))
    @Query("""
            SELECT o FROM Outbox o
            WHERE o.status = :status
              AND o.processableAfter <= :now
              AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
            """)
    List<Outbox> findPendingOutboxesWithSkipLock(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
