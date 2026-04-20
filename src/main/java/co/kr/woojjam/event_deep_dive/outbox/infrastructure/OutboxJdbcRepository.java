package co.kr.woojjam.event_deep_dive.outbox.infrastructure;

import co.kr.woojjam.event_deep_dive.outbox.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional
    public int resetStaleProcessing(LocalDateTime threshold) {
        return jdbcTemplate.update("""
                UPDATE payment_outbox
                SET status = 'PENDING', processing_started_at = NULL
                WHERE status = 'PROCESSING'
                  AND processing_started_at < ?
                """, threshold);
    }

    public int countPending(LocalDateTime now) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM payment_outbox
                WHERE status = 'PENDING'
                  AND processable_after <= ?
                  AND (next_retry_at IS NULL OR next_retry_at <= ?)
                """, Integer.class, now, now);
        return count != null ? count : 0;
    }

    public int countActiveInstances() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM instance_heartbeat
                WHERE last_seen_at > NOW() - INTERVAL 60 SECOND
                """, Integer.class);
        return count != null && count > 0 ? count : 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimBatch(int limit, LocalDateTime now) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM payment_outbox
                WHERE status = 'PENDING'
                  AND processable_after <= ?
                  AND (next_retry_at IS NULL OR next_retry_at <= ?)
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, Long.class, now, now, limit);

        if (ids.isEmpty()) return ids;

        namedParameterJdbcTemplate.update("""
                UPDATE payment_outbox
                SET status = 'PROCESSING', processing_started_at = NOW()
                WHERE id IN (:ids)
                """, new MapSqlParameterSource("ids", ids));

        return ids;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(Long id) {
        jdbcTemplate.update("""
                UPDATE payment_outbox SET status = 'PROCESSED'
                WHERE id = ?
                """, id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, int retryCount, LocalDateTime nextRetryAt, OutboxStatus status) {
        jdbcTemplate.update("""
                UPDATE payment_outbox
                SET status = ?, retry_count = ?, next_retry_at = ?
                WHERE id = ?
                """, status.name(), retryCount, nextRetryAt, id);
    }
}
