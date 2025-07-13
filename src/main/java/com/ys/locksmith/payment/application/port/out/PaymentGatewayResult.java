package com.ys.locksmith.payment.application.port.out;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentGatewayResult {
    
    private boolean success;
    private String transactionId;
    private String message;
    
    public static PaymentGatewayResult success(String transactionId) {
        return new PaymentGatewayResult(true, transactionId, "결제 성공");
    }
    
    public static PaymentGatewayResult failure(String message) {
        return new PaymentGatewayResult(false, null, message);
    }
}