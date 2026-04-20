package co.kr.woojjam.event_deep_dive.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "instance_heartbeat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceHeartbeat {

    @Id
    @Column(name = "instance_id")
    private String instanceId;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;
}
