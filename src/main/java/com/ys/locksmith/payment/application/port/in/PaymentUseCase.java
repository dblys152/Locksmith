package com.ys.locksmith.payment.application.port.in;

import com.ys.locksmith.payment.domain.Payment;

public interface PaymentUseCase {
    
    Payment processPayment(PaymentCommand command);
    
    Payment getPayment(Long paymentId);
    
    Payment cancelPayment(Long paymentId);
}