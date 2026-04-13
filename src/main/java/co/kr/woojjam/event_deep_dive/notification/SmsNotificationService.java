package co.kr.woojjam.event_deep_dive.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class SmsNotificationService {

    /**
     * 구장주에게 매치 신청 완료 문자를 전송한다.
     * 실제 SMS API 호출을 Thread.sleep으로 모킹한다.
     */
    public void sendToStadiumOwner(String phone, String userName, String matchTitle, BigDecimal amount) {
        String message = String.format("[풋살 매칭] %s님이 '%s' 매치에 신청하였습니다. 결제금액: %s원",
                userName, matchTitle, amount.toPlainString());

        log.info("[SMS] 구장주({}) 문자 전송 시작 - 메시지: {}", phone, message);
        try {
            Thread.sleep(500); // 외부 SMS API 호출 모킹
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SMS 전송 중단", e);
        }
        log.info("[SMS] 구장주({}) 문자 전송 완료", phone);
    }
}
