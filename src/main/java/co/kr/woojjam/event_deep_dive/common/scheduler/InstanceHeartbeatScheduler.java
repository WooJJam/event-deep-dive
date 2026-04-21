package co.kr.woojjam.event_deep_dive.common.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.scheduler.type", havingValue = "skip-locked")
public class InstanceHeartbeatScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final String instanceId;

    public InstanceHeartbeatScheduler(JdbcTemplate jdbcTemplate, @Qualifier("instanceId") String instanceId) {
        this.jdbcTemplate = jdbcTemplate;
        this.instanceId = instanceId;
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        jdbcTemplate.update("""
                INSERT INTO instance_heartbeat (instance_id, last_seen_at)
                VALUES (?, NOW())
                ON DUPLICATE KEY UPDATE last_seen_at = NOW()
                """, instanceId);
        log.debug("[Heartbeat] instanceId={}", instanceId);
    }
}
