package co.kr.woojjam.event_deep_dive.match.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateMatchRequest(
        String title,
        String stadiumName,
        Long stadiumOwnerId,
        String stadiumOwnerPhone,
        LocalDateTime matchDate,
        int maxParticipants,
        BigDecimal fee
) {
}
