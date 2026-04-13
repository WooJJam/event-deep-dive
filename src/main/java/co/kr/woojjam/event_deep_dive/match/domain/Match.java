package co.kr.woojjam.event_deep_dive.match.domain;

import co.kr.woojjam.event_deep_dive.match.application.dto.CreateMatchRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "futsal_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String stadiumName;

    @Column(nullable = false)
    private Long stadiumOwnerId;

    @Column(nullable = false)
    private String stadiumOwnerPhone;

    @Column(nullable = false)
    private LocalDateTime matchDate;

    @Column(nullable = false)
    private int maxParticipants;

    @Column(nullable = false)
    private int currentParticipants;

    @Column(nullable = false)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Match(String title, String stadiumName, Long stadiumOwnerId, String stadiumOwnerPhone,
                  LocalDateTime matchDate, int maxParticipants, BigDecimal fee) {
        this.title = title;
        this.stadiumName = stadiumName;
        this.stadiumOwnerId = stadiumOwnerId;
        this.stadiumOwnerPhone = stadiumOwnerPhone;
        this.matchDate = matchDate;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = 0;
        this.fee = fee;
        this.status = MatchStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public static Match from(CreateMatchRequest request) {
        return Match.builder()
                .title(request.title())
                .stadiumName(request.stadiumName())
                .stadiumOwnerId(request.stadiumOwnerId())
                .stadiumOwnerPhone(request.stadiumOwnerPhone())
                .matchDate(request.matchDate())
                .maxParticipants(request.maxParticipants())
                .fee(request.fee())
                .build();
    }

    public void validateApplicable() {
        if (this.status != MatchStatus.OPEN) {
            throw new IllegalStateException("신청 가능한 매치가 아닙니다. 현재 상태: " + this.status);
        }
        if (this.currentParticipants >= this.maxParticipants) {
            throw new IllegalStateException("매치 정원이 가득 찼습니다.");
        }
    }

    public void increaseParticipants() {
        validateApplicable();
        this.currentParticipants++;
        if (this.currentParticipants >= this.maxParticipants) {
            this.status = MatchStatus.CLOSED;
        }
    }

    public int remainingSlots() {
        return this.maxParticipants - this.currentParticipants;
    }
}
