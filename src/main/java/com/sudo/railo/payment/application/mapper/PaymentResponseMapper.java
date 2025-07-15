package com.sudo.railo.payment.application.mapper;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.PaymentResult;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;

import java.math.BigDecimal;

/**
 * Payment 응답 매핑 유틸리티
 * 
 * PaymentResult와 PaymentContext를 PaymentExecuteResponse로 변환
 */
public class PaymentResponseMapper {
    
    private PaymentResponseMapper() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
    
    /**
     * PaymentResult를 PaymentExecuteResponse로 변환
     * 
     * @param result 결제 실행 결과
     * @param context 결제 컨텍스트
     * @return 결제 실행 응답 DTO
     */
    public static PaymentExecuteResponse from(PaymentResult result, PaymentContext context) {
        Payment payment = result.getPayment();
        
        String message = buildSuccessMessage(payment, context);
        
        return PaymentExecuteResponse.builder()
            .paymentId(payment.getId())
            .externalOrderId(payment.getExternalOrderId())
            .paymentStatus(payment.getPaymentStatus())
            .amountPaid(payment.getAmountPaid())
            .mileagePointsUsed(payment.getMileagePointsUsed())
            .mileageAmountDeducted(payment.getMileageAmountDeducted())
            .mileageToEarn(payment.getMileageToEarn())
            .result(PaymentExecuteResponse.PaymentResult.builder()
                .success(result.isSuccess())
                .message(message)
                .build())
            .build();
    }
    
    /**
     * Payment 엔티티를 PaymentExecuteResponse로 변환 (조회용)
     * 
     * @param payment 결제 엔티티
     * @return 결제 실행 응답 DTO
     */
    public static PaymentExecuteResponse from(Payment payment) {
        boolean isSuccess = payment.getPaymentStatus() == PaymentExecutionStatus.SUCCESS;
        String message = isSuccess ? "결제가 완료되었습니다." : "결제 처리 중입니다.";
        
        return PaymentExecuteResponse.builder()
            .paymentId(payment.getId())
            .externalOrderId(payment.getExternalOrderId())
            .paymentStatus(payment.getPaymentStatus())
            .amountPaid(payment.getAmountPaid())
            .mileagePointsUsed(payment.getMileagePointsUsed())
            .mileageAmountDeducted(payment.getMileageAmountDeducted())
            .mileageToEarn(payment.getMileageToEarn())
            .result(PaymentExecuteResponse.PaymentResult.builder()
                .success(isSuccess)
                .message(message)
                .build())
            .build();
    }
    
    /**
     * 성공 메시지 생성
     */
    private static String buildSuccessMessage(Payment payment, PaymentContext context) {
        if (!context.isForMember()) {
            return "비회원 결제가 완료되었습니다.";
        }
        
        BigDecimal mileageToEarn = payment.getMileageToEarn();
        if (mileageToEarn != null && mileageToEarn.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("회원 결제가 완료되었습니다. 마일리지 %s포인트가 적립됩니다.", 
                mileageToEarn);
        }
        
        return "회원 결제가 완료되었습니다.";
    }
}