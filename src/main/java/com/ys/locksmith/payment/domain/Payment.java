package com.ys.locksmith.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;
    
    @Embedded
    private Money amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public static Payment create(Long userId, String orderId, Money amount, PaymentMethod paymentMethod) {
        return new Payment(
            null,
            userId,
            orderId,
            amount,
            paymentMethod,
            PaymentStatus.PENDING,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 상태가 PENDING이 아닙니다: " + this.status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void fail() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 상태가 PENDING이 아닙니다: " + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void cancel() {
        if (this.status == PaymentStatus.FAILED) {
            throw new IllegalStateException("실패한 결제는 취소할 수 없습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }
    
    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }
}
