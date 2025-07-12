package com.sudo.railo.payment.application.factory;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.domain.entity.CashReceipt;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Payment 엔티티 팩토리
 * 
 * PaymentContext를 기반으로 Payment 엔티티를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFactory {
    
    private final PasswordEncoder passwordEncoder;
    private final com.sudo.railo.member.infra.MemberRepository memberRepository;
    
    /**
     * PaymentContext로부터 Payment 엔티티 생성
     * 
     * @param context 검증된 결제 컨텍스트
     * @return Payment 엔티티
     */
    public Payment create(PaymentContext context) {
        log.debug("Payment 엔티티 생성 시작 - reservationId: {}", context.getReservationId());
        
        // 마일리지 정보 추출
        BigDecimal mileagePointsUsed = extractMileagePointsUsed(context);
        BigDecimal mileageAmountDeducted = extractMileageAmountDeducted(context);
        
        // 최종 결제 금액 계산
        BigDecimal finalPayableAmount = calculateFinalPayableAmount(context, mileageAmountDeducted);
        
        // 마일리지 적립 예정 금액 계산
        BigDecimal mileageToEarn = calculateMileageToEarn(context, finalPayableAmount);
        
        // Payment Builder 생성
        Payment.PaymentBuilder builder = Payment.builder()
                .reservationId(context.getReservationId())
                .externalOrderId(generateExternalOrderId(context))
                .amountOriginalTotal(context.getCalculation().getAmountOriginalTotal())
                .totalDiscountAmountApplied(context.getCalculation().getTotalDiscountAmountApplied())
                .mileagePointsUsed(mileagePointsUsed)
                .mileageAmountDeducted(mileageAmountDeducted)
                .amountPaid(finalPayableAmount)
                .mileageToEarn(mileageToEarn)
                .paymentMethod(extractPaymentMethod(context))
                .pgProvider(context.getRequest().getPaymentMethod().getPgProvider())
                .paymentStatus(PaymentExecutionStatus.PENDING)
                .idempotencyKey(context.getIdempotencyKey());
        
        // 회원/비회원별 정보 설정
        if (context.isForMember()) {
            setMemberInfo(builder, context);
        } else {
            setNonMemberInfo(builder, context);
        }
        
        // 현금영수증 정보 설정
        setCashReceiptInfo(builder, context);
        
        Payment payment = builder.build();
        
        log.debug("Payment 엔티티 생성 완료 - amountPaid: {}, mileageUsed: {}", 
                payment.getAmountPaid(), payment.getMileagePointsUsed());
        
        return payment;
    }
    
    /**
     * 사용 마일리지 포인트 추출
     */
    private BigDecimal extractMileagePointsUsed(PaymentContext context) {
        if (context.getMileageResult() == null || !context.getMileageResult().isValid()) {
            return BigDecimal.ZERO;
        }
        return context.getMileageResult().getUsageAmount();
    }
    
    /**
     * 마일리지 차감 금액 추출
     */
    private BigDecimal extractMileageAmountDeducted(PaymentContext context) {
        if (context.getMileageResult() == null || !context.getMileageResult().isValid()) {
            return BigDecimal.ZERO;
        }
        // 1포인트 = 1원 고정
        return context.getMileageResult().getUsageAmount();
    }
    
    /**
     * 최종 결제 금액 계산
     */
    private BigDecimal calculateFinalPayableAmount(PaymentContext context, BigDecimal mileageAmountDeducted) {
        BigDecimal finalAmount = context.getFinalPayableAmount().subtract(mileageAmountDeducted);
        
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentValidationException("최종 결제 금액이 음수입니다");
        }
        
        return finalAmount;
    }
    
    /**
     * 마일리지 적립 예정 금액 계산
     */
    private BigDecimal calculateMileageToEarn(PaymentContext context, BigDecimal finalPayableAmount) {
        if (!context.isForMember()) {
            return BigDecimal.ZERO;
        }
        
        // 기본 적립률 1% (정책에 따라 변경 가능)
        BigDecimal earningRate = BigDecimal.valueOf(0.01);
        
        return finalPayableAmount
                .multiply(earningRate)
                .setScale(0, RoundingMode.DOWN);
    }
    
    /**
     * 외부 주문 ID 생성
     */
    private String generateExternalOrderId(PaymentContext context) {
        // 계산 응답에 externalOrderId가 있으면 사용, 없으면 생성
        String externalOrderId = context.getCalculation().getExternalOrderId();
        if (externalOrderId != null && !externalOrderId.trim().isEmpty()) {
            return externalOrderId;
        }
        
        // 없으면 타임스탬프 기반 생성
        return "ORD" + System.currentTimeMillis();
    }
    
    /**
     * 결제 수단 추출
     */
    private PaymentMethod extractPaymentMethod(PaymentContext context) {
        String methodType = context.getRequest().getPaymentMethod().getType();
        try {
            return PaymentMethod.valueOf(methodType);
        } catch (IllegalArgumentException e) {
            throw new PaymentValidationException("지원하지 않는 결제 수단입니다: " + methodType);
        }
    }
    
    /**
     * 회원 정보 설정
     */
    private void setMemberInfo(Payment.PaymentBuilder builder, PaymentContext context) {
        // Member 엔티티 조회 및 설정
        Long memberId = context.getMemberId();
        com.sudo.railo.member.domain.Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new PaymentValidationException("회원을 찾을 수 없습니다. ID: " + memberId));
        
        builder.member(member);
        log.debug("회원 정보 설정 완료 - memberId: {}, memberName: {}", 
                member.getId(), member.getName());
    }
    
    /**
     * 비회원 정보 설정
     */
    private void setNonMemberInfo(Payment.PaymentBuilder builder, PaymentContext context) {
        var nonMemberInfo = context.getRequest().getNonMemberInfo();
        
        if (nonMemberInfo == null) {
            throw new PaymentValidationException("비회원 정보가 필요합니다");
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(nonMemberInfo.getPassword());
        
        builder.nonMemberName(nonMemberInfo.getName())
               .nonMemberPhone(normalizePhoneNumber(nonMemberInfo.getPhone()))
               .nonMemberPassword(encodedPassword);
    }
    
    /**
     * 현금영수증 정보 설정
     */
    private void setCashReceiptInfo(Payment.PaymentBuilder builder, PaymentContext context) {
        var cashReceiptRequest = context.getRequest().getCashReceiptInfo();
        
        if (cashReceiptRequest == null || !cashReceiptRequest.isRequested()) {
            builder.cashReceipt(CashReceipt.notRequested());
            return;
        }
        
        CashReceipt cashReceipt;
        if ("personal".equals(cashReceiptRequest.getType())) {
            cashReceipt = CashReceipt.createPersonalReceipt(
                normalizePhoneNumber(cashReceiptRequest.getPhoneNumber())
            );
        } else if ("business".equals(cashReceiptRequest.getType())) {
            cashReceipt = CashReceipt.createBusinessReceipt(
                cashReceiptRequest.getBusinessNumber()
            );
        } else {
            throw new PaymentValidationException("지원하지 않는 현금영수증 타입입니다: " + cashReceiptRequest.getType());
        }
        
        builder.cashReceipt(cashReceipt);
    }
    
    /**
     * 전화번호 정규화
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }
}