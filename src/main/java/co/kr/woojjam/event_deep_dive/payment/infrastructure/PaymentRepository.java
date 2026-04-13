package co.kr.woojjam.event_deep_dive.payment.infrastructure;

import co.kr.woojjam.event_deep_dive.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
