package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.NonMemberVerifyRequest;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.NonMemberService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비회원 결제 관련 애플리케이션 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NonMemberPaymentService {
    
    private final PaymentRepository paymentRepository;
    private final NonMemberService nonMemberService;
    
    /**
     * 비회원 결제 정보 확인
     */
    public PaymentInfoResponse verifyNonMemberPayment(NonMemberVerifyRequest request) {
        
        // 1. 예약 번호로 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(request.getReservationId())
            .orElseThrow(() -> new PaymentValidationException("해당 예약의 결제 정보를 찾을 수 없습니다"));
        
        // 2. 회원 결제인지 확인
        if (payment.getMemberId() != null) {
            throw new PaymentValidationException("회원 결제입니다. 로그인 후 조회해주세요");
        }
        
        // 3. 비회원 정보 검증
        boolean isValid = nonMemberService.validateNonMemberInfo(
            request.getName(), 
            request.getPhone(), 
            request.getPassword(), 
            payment
        );
        
        if (!isValid) {
            log.warn("비회원 정보 검증 실패 - 예약번호: {}, 요청 이름: {}", 
                    request.getReservationId(), request.getName());
            throw new PaymentValidationException("입력한 정보가 일치하지 않습니다");
        }
        
        // 4. 응답 생성 (민감한 정보 마스킹)
        return PaymentInfoResponse.builder()
            .paymentId(payment.getPaymentId())
            .reservationId(payment.getReservationId())
            .externalOrderId(payment.getExternalOrderId())
            .amountOriginalTotal(payment.getAmountOriginalTotal())
            .totalDiscountAmountApplied(payment.getTotalDiscountAmountApplied())
            .mileageAmountDeducted(payment.getMileageAmountDeducted())
            .amountPaid(payment.getAmountPaid())
            .paymentStatus(payment.getPaymentStatus())
            .paymentMethod(payment.getPaymentMethod())
            .pgProvider(payment.getPgProvider())
            .pgTransactionId(payment.getPgTransactionId())
            .pgApprovalNo(payment.getPgApprovalNo())
            .receiptUrl(payment.getReceiptUrl())
            .paidAt(payment.getPaidAt())
            .createdAt(payment.getCreatedAt())
            .nonMemberName(payment.getNonMemberName())
            .nonMemberPhoneMasked(maskPhoneNumber(payment.getNonMemberPhone()))
            .build();
    }
    
    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
} 