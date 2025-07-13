package com.ys.locksmith.payment.adapter.out;

import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {
    
    @Override
    Optional<Payment> findByOrderId(String orderId);
    
    @Override
    boolean existsByOrderId(String orderId);
}