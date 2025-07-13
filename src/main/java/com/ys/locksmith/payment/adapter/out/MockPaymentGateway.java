package com.ys.locksmith.payment.adapter.out;

import com.ys.locksmith.payment.application.port.out.ExternalPaymentGateway;
import com.ys.locksmith.payment.application.port.out.PaymentGatewayResult;
import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class MockPaymentGateway implements ExternalPaymentGateway {
    
    @Override
    public PaymentGatewayResult processPayment(String orderId, Money amount, PaymentMethod paymentMethod) {
        log.info("Mock 결제 게이트웨이 호출: orderId={}, amount={}, method={}", orderId, amount, paymentMethod);
        
        try {
            Thread.sleep(100);
            
            if (amount.getAmount().doubleValue() > 1000000) {
                return PaymentGatewayResult.failure("결제 한도를 초과했습니다.");
            }
            
            String transactionId = UUID.randomUUID().toString();
            return PaymentGatewayResult.success(transactionId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentGatewayResult.failure("결제 처리 중 오류가 발생했습니다.");
        }
    }
    
    @Override
    public boolean cancelPayment(String orderId) {
        log.info("Mock 결제 취소 호출: orderId={}", orderId);
        
        try {
            Thread.sleep(50);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}