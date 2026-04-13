package co.kr.woojjam.event_deep_dive.match.application;

import co.kr.woojjam.event_deep_dive.match.application.dto.CreateMatchRequest;
import co.kr.woojjam.event_deep_dive.match.application.dto.MatchApplyRequest;
import co.kr.woojjam.event_deep_dive.match.application.dto.MatchResponse;
import co.kr.woojjam.event_deep_dive.match.domain.Match;
import co.kr.woojjam.event_deep_dive.match.domain.MatchStatus;
import co.kr.woojjam.event_deep_dive.match.infrastructure.MatchRepository;
import co.kr.woojjam.event_deep_dive.payment.application.PaymentService;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentRequest;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final PaymentService paymentService;

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request) {
        Match match = Match.from(request);
        matchRepository.save(match);
        log.info("[Match] 매치 생성 완료 - matchId: {}, title: {}", match.getId(), match.getTitle());
        return MatchResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getOpenMatches() {
        return matchRepository.findByStatusOrderByMatchDateAsc(MatchStatus.OPEN)
                .stream()
                .map(MatchResponse::from)
                .toList();
    }

    /**
     * 매치 신청 + 결제 처리.
     * Match에서 구장 정보를 꺼내 PaymentRequest를 구성하고 PaymentService에 위임한다.
     * 참가자 수 증가도 동일 트랜잭션에서 처리한다.
     */
    @Transactional
    public PaymentResponse applyMatch(Long matchId, MatchApplyRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매치입니다. id: " + matchId));

        match.validateApplicable();

        PaymentRequest paymentRequest = new PaymentRequest(
                match.getId(),
                match.getTitle(),
                request.userId(),
                request.userName(),
                request.userFcmToken(),
                match.getStadiumOwnerId(),
                match.getStadiumOwnerPhone(),
                match.getFee(),
                request.idempotencyKey()
        );

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

        match.increaseParticipants();
        log.info("[Match] 매치 신청 완료 - matchId: {}, userId: {}, 잔여 슬롯: {}",
                matchId, request.userId(), match.remainingSlots());

        return paymentResponse;
    }
}
