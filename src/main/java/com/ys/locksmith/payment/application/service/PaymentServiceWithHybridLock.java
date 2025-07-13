package com.ys.locksmith.payment.application.service;

import com.ys.locksmith.common.lock.DistributedLockUtil;
import com.ys.locksmith.payment.application.port.in.PaymentCommand;
import com.ys.locksmith.payment.application.port.in.PaymentUseCase;
import com.ys.locksmith.payment.application.port.out.ExternalPaymentGateway;
import com.ys.locksmith.payment.application.port.out.PaymentGatewayResult;
import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("paymentServiceWithHybridLock")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceWithHybridLock implements PaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final ExternalPaymentGateway paymentGateway;
    private final DistributedLockUtil distributedLockUtil;
    
    @Override
    public Payment processPayment(PaymentCommand command) {
        validatePaymentCommand(command);
        
        String lockKey = "payment:order:" + command.getOrderId();
        
        return distributedLockUtil.executeWithLock(lockKey, () -> {
            log.info("하이브리드 락(분산락 + 비관적 락)을 사용한 결제 처리 시작: orderId={}", command.getOrderId());
            
            if (paymentRepository.existsByOrderId(command.getOrderId())) {
                throw new IllegalArgumentException("이미 존재하는 주문 ID입니다: " + command.getOrderId());
            }
            
            Payment payment = Payment.create(
                command.getUserId(),
                command.getOrderId(),
                command.getAmount(),
                command.getPaymentMethod()
            );
            
            Payment savedPayment = paymentRepository.save(payment);
            
            try {
                PaymentGatewayResult result = paymentGateway.processPayment(
                    command.getOrderId(),
                    command.getAmount(),
                    command.getPaymentMethod()
                );
                
                if (result.isSuccess()) {
                    savedPayment.complete();
                    log.info("결제 성공: orderId={}, transactionId={}", command.getOrderId(), result.getTransactionId());
                } else {
                    savedPayment.fail();
                    log.warn("결제 실패: orderId={}, message={}", command.getOrderId(), result.getMessage());
                }
                
                return paymentRepository.save(savedPayment);
                
            } catch (Exception e) {
                savedPayment.fail();
                paymentRepository.save(savedPayment);
                log.error("결제 처리 중 오류 발생: orderId={}", command.getOrderId(), e);
                throw new RuntimeException("결제 처리 중 오류가 발생했습니다.", e);
            }
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + paymentId));
    }
    
    @Override
    public Payment cancelPayment(Long paymentId) {
        String lockKey = "payment:cancel:" + paymentId;
        
        return distributedLockUtil.executeWithLock(lockKey, () -> {
            log.info("하이브리드 락을 사용한 결제 취소 처리 시작: paymentId={}", paymentId);
            
            Payment payment = paymentRepository.findByIdWithPessimisticLock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + paymentId));
            
            if (!payment.isCompleted()) {
                throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
            }
            
            try {
                boolean cancelResult = paymentGateway.cancelPayment(payment.getOrderId());
                
                if (cancelResult) {
                    payment.cancel();
                    log.info("결제 취소 성공: orderId={}", payment.getOrderId());
                } else {
                    log.warn("결제 취소 실패: orderId={}", payment.getOrderId());
                    throw new RuntimeException("결제 취소에 실패했습니다.");
                }
                
                return paymentRepository.save(payment);
                
            } catch (Exception e) {
                log.error("결제 취소 중 오류 발생: orderId={}", payment.getOrderId(), e);
                throw new RuntimeException("결제 취소 중 오류가 발생했습니다.", e);
            }
        });
    }
    
    private void validatePaymentCommand(PaymentCommand command) {
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (command.getAmount() == null || !command.getAmount().isPositive()) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        }
        if (command.getPaymentMethod() == null) {
            throw new IllegalArgumentException("결제 방법은 필수입니다.");
        }
    }
}