package com.ys.locksmith.payment.application.port.out;

import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.PaymentMethod;

public interface ExternalPaymentGateway {
    
    PaymentGatewayResult processPayment(String orderId, Money amount, PaymentMethod paymentMethod);
    
    boolean cancelPayment(String orderId);
}