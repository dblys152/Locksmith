package com.ys.locksmith.payment.application.service;

import com.ys.locksmith.payment.application.port.in.PaymentCommand;
import com.ys.locksmith.payment.application.port.out.ExternalPaymentGateway;
import com.ys.locksmith.payment.application.port.out.PaymentGatewayResult;
import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.Payment;
import com.ys.locksmith.payment.domain.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("결제 서비스 동시성 테스트")
class PaymentServiceConcurrencyTest {
    
    @Autowired
    private PaymentServiceWithManualLock paymentServiceWithManualLock;
    
    @MockitoBean
    private PaymentRepository paymentRepository;
    
    @MockitoBean
    private ExternalPaymentGateway paymentGateway;
    
    @Test
    @DisplayName("동일한 주문 ID로 동시 결제 요청 시 하나만 성공한다")
    void concurrentPaymentWithSameOrderId() throws ExecutionException, InterruptedException {
        // given
        String orderId = "CONCURRENT-ORDER-001";
        PaymentCommand command1 = PaymentCommand.of(1L, orderId, Money.krw(10000), PaymentMethod.CREDIT_CARD);
        PaymentCommand command2 = PaymentCommand.of(2L, orderId, Money.krw(20000), PaymentMethod.DEBIT_CARD);
        
        AtomicInteger saveCallCount = new AtomicInteger(0);
        AtomicInteger existsCallCount = new AtomicInteger(0);
        
        given(paymentRepository.existsByOrderId(orderId)).willAnswer(invocation -> {
            int callNumber = existsCallCount.incrementAndGet();
            return callNumber > 1; // 첫 번째 호출은 false, 이후 호출은 true
        });
        
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            saveCallCount.incrementAndGet();
            Payment payment = invocation.getArgument(0);
            return payment;
        });
        
        given(paymentGateway.processPayment(anyString(), any(Money.class), any(PaymentMethod.class)))
            .willReturn(PaymentGatewayResult.success("TXN-001"));
        
        // when
        CompletableFuture<Payment> future1 = CompletableFuture.supplyAsync(() -> 
            paymentServiceWithManualLock.processPayment(command1));
        
        CompletableFuture<Payment> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return paymentServiceWithManualLock.processPayment(command2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // then
        Payment result1 = future1.get();
        
        assertThatThrownBy(future2::get)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 주문 ID입니다");
        
        assertThat(result1.getOrderId()).isEqualTo(orderId);
        assertThat(saveCallCount.get()).isGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("서로 다른 주문 ID로 동시 결제 요청 시 모두 성공한다")
    void concurrentPaymentWithDifferentOrderId() throws ExecutionException, InterruptedException {
        // given
        PaymentCommand command1 = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        PaymentCommand command2 = PaymentCommand.of(2L, "ORDER-002", Money.krw(20000), PaymentMethod.DEBIT_CARD);
        
        given(paymentRepository.existsByOrderId(anyString())).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentGateway.processPayment(anyString(), any(Money.class), any(PaymentMethod.class)))
            .willReturn(PaymentGatewayResult.success("TXN-001"));
        
        // when
        CompletableFuture<Payment> future1 = CompletableFuture.supplyAsync(() -> 
            paymentServiceWithManualLock.processPayment(command1));
        
        CompletableFuture<Payment> future2 = CompletableFuture.supplyAsync(() -> 
            paymentServiceWithManualLock.processPayment(command2));
        
        // then
        Payment result1 = future1.get();
        Payment result2 = future2.get();
        
        assertThat(result1.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result2.getOrderId()).isEqualTo("ORDER-002");
    }
}