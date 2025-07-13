package com.ys.locksmith.payment.domain;

public enum PaymentStatus {
    PENDING("대기"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}