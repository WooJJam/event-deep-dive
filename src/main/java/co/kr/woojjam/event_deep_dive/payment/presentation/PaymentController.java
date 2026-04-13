package co.kr.woojjam.event_deep_dive.payment.presentation;

import co.kr.woojjam.event_deep_dive.payment.application.PaymentService;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentRequest;
import co.kr.woojjam.event_deep_dive.payment.application.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}
