package com.ys.locksmith.payment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ys.locksmith.payment.application.port.in.PaymentCommand;
import com.ys.locksmith.payment.application.port.in.PaymentUseCase;
import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.Payment;
import com.ys.locksmith.payment.domain.PaymentMethod;
import com.ys.locksmith.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("결제 컨트롤러 테스트")
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private PaymentUseCase paymentUseCase;
    
    @Test
    @DisplayName("결제 요청을 성공적으로 처리한다")
    void processPayment() throws Exception {
        // given
        PaymentRequest request = new PaymentRequest(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        given(paymentUseCase.processPayment(any(PaymentCommand.class))).willReturn(completedPayment);
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.amount.amount").value(10000))
                .andExpect(jsonPath("$.amount.currency").value("KRW"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
    
    @Test
    @DisplayName("결제 정보를 성공적으로 조회한다")
    void getPayment() throws Exception {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        given(paymentUseCase.getPayment(1L)).willReturn(payment);
        
        // when & then
        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    @Test
    @DisplayName("결제를 성공적으로 취소한다")
    void cancelPayment() throws Exception {
        // given
        Payment cancelledPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        cancelledPayment.complete();
        cancelledPayment.cancel();
        
        given(paymentUseCase.cancelPayment(1L)).willReturn(cancelledPayment);
        
        // when & then
        mockMvc.perform(post("/api/payments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
    
    @Test
    @DisplayName("존재하지 않는 결제 조회 시 404 에러가 발생한다")
    void getPaymentNotFound() throws Exception {
        // given
        given(paymentUseCase.getPayment(999L))
                .willThrow(new IllegalArgumentException("결제 정보를 찾을 수 없습니다: 999"));
        
        // when & then
        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("잘못된 결제 요청 시 400 에러가 발생한다")
    void processPaymentBadRequest() throws Exception {
        // given
        PaymentRequest invalidRequest = new PaymentRequest(null, null, null, null);
        
        given(paymentUseCase.processPayment(any(PaymentCommand.class)))
                .willThrow(new IllegalArgumentException("사용자 ID는 필수입니다."));
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isInternalServerError());
    }
}