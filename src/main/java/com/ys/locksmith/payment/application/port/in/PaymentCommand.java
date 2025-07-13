package com.ys.locksmith.payment.application.port.in;

import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.PaymentMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentCommand {
    
    private Long userId;
    private String orderId;
    private Money amount;
    private PaymentMethod paymentMethod;
    
    public static PaymentCommand of(Long userId, String orderId, Money amount, PaymentMethod paymentMethod) {
        return new PaymentCommand(userId, orderId, amount, paymentMethod);
    }
}