package co.kr.woojjam.event_deep_dive.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmNotificationService {

    /**
     * 매치를 신청한 사용자에게 FCM 푸시 알림을 전송한다.
     * 실제 FCM API 호출을 Thread.sleep으로 모킹한다.
     */
    public void sendToUser(String fcmToken, String userName, String matchTitle) {
        String title = "매치 신청 완료";
        String body = String.format("%s님, '%s' 매치 신청이 완료되었습니다!", userName, matchTitle);

        log.info("[FCM] 사용자 푸시 전송 시작 - token: {}, title: {}, body: {}", fcmToken, title, body);
        try {
            Thread.sleep(300); // 외부 FCM API 호출 모킹
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("FCM 전송 중단", e);
        }
        log.info("[FCM] 사용자 푸시 전송 완료 - token: {}", fcmToken);
    }
}
