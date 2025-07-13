package com.ys.locksmith.payment.adapter.in.web;

import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.PaymentMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentRequest {
    
    private Long userId;
    private String orderId;
    private Money amount;
    private PaymentMethod paymentMethod;
}