package co.kr.woojjam.event_deep_dive.match.infrastructure;

import co.kr.woojjam.event_deep_dive.match.domain.Match;
import co.kr.woojjam.event_deep_dive.match.domain.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByStatusOrderByMatchDateAsc(MatchStatus status);
}
