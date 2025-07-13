package com.ys.locksmith.payment.adapter.in.web;

import com.ys.locksmith.payment.application.port.in.PaymentCommand;
import com.ys.locksmith.payment.application.port.in.PaymentUseCase;
import com.ys.locksmith.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentUseCase paymentUseCase;
    
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentCommand command = PaymentCommand.of(
            request.getUserId(),
            request.getOrderId(),
            request.getAmount(),
            request.getPaymentMethod()
        );
        
        Payment payment = paymentUseCase.processPayment(command);
        
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentUseCase.getPayment(paymentId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
    
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
        Payment payment = paymentUseCase.cancelPayment(paymentId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}