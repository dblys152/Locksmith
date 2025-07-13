package com.ys.locksmith.payment.adapter.out;

import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {
    
    @Override
    Optional<Payment> findByOrderId(String orderId);
    
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithPessimisticLock(@Param("id") Long id);
    
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithPessimisticLock(@Param("orderId") String orderId);
    
    @Override
    boolean existsByOrderId(String orderId);
}