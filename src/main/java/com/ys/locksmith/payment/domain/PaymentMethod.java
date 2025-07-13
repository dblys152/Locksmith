package com.ys.locksmith.payment.domain;

public enum PaymentMethod {
    CREDIT_CARD("신용카드"),
    DEBIT_CARD("체크카드"),
    BANK_TRANSFER("계좌이체"),
    MOBILE_PAYMENT("모바일 결제"),
    VIRTUAL_ACCOUNT("가상계좌");
    
    private final String description;
    
    PaymentMethod(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}