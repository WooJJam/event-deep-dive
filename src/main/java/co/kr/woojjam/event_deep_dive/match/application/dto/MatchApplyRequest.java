package co.kr.woojjam.event_deep_dive.match.application.dto;

public record MatchApplyRequest(
        Long userId,
        String userName,
        String userFcmToken,
        String idempotencyKey
) {
}
