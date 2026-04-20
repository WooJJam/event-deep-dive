package co.kr.woojjam.event_deep_dive.outbox.scheduler;

import co.kr.woojjam.event_deep_dive.outbox.scheduler.worker.SkipLockedOutboxWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.type", havingValue = "skip-locked")
public class SkipLockedOutboxScheduler {

    private final SkipLockedOutboxWorker worker;

    @Scheduled(fixedDelay = 30_000)
    public void trigger() {
        worker.process();
    }
}
