package co.kr.woojjam.event_deep_dive.outbox.scheduler.skiplock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.type", havingValue = "skip-locked")
public class InstanceHeartbeatScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Value("${APP_INSTANCE_ID:local}")
    private String instanceId;

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
