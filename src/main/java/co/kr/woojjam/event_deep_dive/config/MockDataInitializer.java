package co.kr.woojjam.event_deep_dive.config;

import co.kr.woojjam.event_deep_dive.match.application.dto.CreateMatchRequest;
import co.kr.woojjam.event_deep_dive.match.domain.Match;
import co.kr.woojjam.event_deep_dive.match.infrastructure.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataInitializer implements ApplicationRunner {

    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (matchRepository.count() > 0) {
            return;
        }

        matchRepository.save(Match.from(new CreateMatchRequest(
                "강남 풋살 5vs5",
                "강남 풋살 파크",
                1L,
                "010-1111-2222",
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0),
                10,
                new BigDecimal("15000")
        )));

        matchRepository.save(Match.from(new CreateMatchRequest(
                "홍대 풋살 6vs6",
                "홍대 실내 풋살장",
                2L,
                "010-3333-4444",
                LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0),
                12,
                new BigDecimal("18000")
        )));

        matchRepository.save(Match.from(new CreateMatchRequest(
                "잠실 풋살 5vs5",
                "잠실 스포츠 센터",
                3L,
                "010-5555-6666",
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                10,
                new BigDecimal("12000")
        )));

        log.info("[MockData] 테스트용 풋살 매치 3건 생성 완료");
    }
}
