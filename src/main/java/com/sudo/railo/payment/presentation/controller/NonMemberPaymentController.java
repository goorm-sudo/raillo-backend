package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.NonMemberVerifyRequest;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.application.service.NonMemberPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 비회원 결제 관련 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/non-member/payments")
@RequiredArgsConstructor
@Slf4j
public class NonMemberPaymentController {
    
    private final NonMemberPaymentService nonMemberPaymentService;
    
    /**
     * 비회원 결제 내역 확인
     * POST /api/v1/non-member/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentInfoResponse> verifyNonMemberPayment(
            @Valid @RequestBody NonMemberVerifyRequest request) {
        
        log.info("비회원 결제 확인 요청 - 예약번호: {}, 이름: {}", 
                request.getReservationId(), request.getName());
        
        PaymentInfoResponse response = nonMemberPaymentService.verifyNonMemberPayment(request);
        
        log.info("비회원 결제 확인 완료 - 결제ID: {}, 상태: {}", 
                response.getPaymentId(), response.getPaymentStatus());
        
        return ResponseEntity.ok(response);
    }
} 