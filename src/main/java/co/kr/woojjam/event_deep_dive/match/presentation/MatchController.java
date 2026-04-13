package co.kr.woojjam.event_deep_dive.match.presentation;

import co.kr.woojjam.event_deep_dive.match.application.MatchService;
import co.kr.woojjam.event_deep_dive.match.application.dto.CreateMatchRequest;
import co.kr.woojjam.event_deep_dive.match.application.dto.MatchApplyRequest;
import co.kr.woojjam.event_deep_dive.match.application.dto.MatchResponse;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(@RequestBody CreateMatchRequest request) {
        return ResponseEntity.ok(matchService.createMatch(request));
    }

    @GetMapping
    public ResponseEntity<List<MatchResponse>> getOpenMatches() {
        return ResponseEntity.ok(matchService.getOpenMatches());
    }

    @PostMapping("/{matchId}/apply")
    public ResponseEntity<PaymentResponse> applyMatch(
            @PathVariable Long matchId,
            @RequestBody MatchApplyRequest request) {
        return ResponseEntity.ok(matchService.applyMatch(matchId, request));
    }
}
