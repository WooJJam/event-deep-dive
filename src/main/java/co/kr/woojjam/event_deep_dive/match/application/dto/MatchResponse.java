package co.kr.woojjam.event_deep_dive.match.application.dto;

import co.kr.woojjam.event_deep_dive.match.domain.Match;
import co.kr.woojjam.event_deep_dive.match.domain.MatchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MatchResponse(
        Long matchId,
        String title,
        String stadiumName,
        String stadiumOwnerPhone,
        LocalDateTime matchDate,
        int maxParticipants,
        int currentParticipants,
        int remainingSlots,
        BigDecimal fee,
        MatchStatus status
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getTitle(),
                match.getStadiumName(),
                match.getStadiumOwnerPhone(),
                match.getMatchDate(),
                match.getMaxParticipants(),
                match.getCurrentParticipants(),
                match.remainingSlots(),
                match.getFee(),
                match.getStatus()
        );
    }
}
