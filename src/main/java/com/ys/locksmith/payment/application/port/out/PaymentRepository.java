package com.ys.locksmith.payment.application.port.out;

import com.ys.locksmith.payment.domain.Payment;

import java.util.Optional;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    Optional<Payment> findByOrderId(String orderId);
    
    Optional<Payment> findByIdWithPessimisticLock(Long id);
    
    Optional<Payment> findByOrderIdWithPessimisticLock(String orderId);
    
    boolean existsByOrderId(String orderId);
}