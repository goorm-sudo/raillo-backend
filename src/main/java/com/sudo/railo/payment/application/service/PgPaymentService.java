package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.PaymentResult.PgPaymentResult;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.exception.PaymentExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PG 결제 처리 서비스
 * 
 * 외부 PG사와의 연동을 담당
 * 현재는 Mock 구현이며, 실제 PG 연동 시 구현체 교체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PgPaymentService {
    
    /**
     * PG 결제 처리
     * 
     * @param payment 결제 엔티티
     * @param context 결제 컨텍스트
     * @return PG 결제 결과
     */
    public PgPaymentResult processPayment(Payment payment, PaymentContext context) {
        log.info("PG 결제 시작 - paymentId: {}, amount: {}, method: {}", 
            payment.getId(), payment.getAmountPaid(), payment.getPaymentMethod());
        
        try {
            // 전액 마일리지 결제인 경우 PG 결제 불필요
            if (payment.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                log.info("전액 마일리지 결제 - PG 결제 생략");
                return PgPaymentResult.builder()
                    .success(true)
                    .pgTransactionId("MILEAGE_ONLY")
                    .pgApprovalNo("MILEAGE_ONLY")
                    .pgMessage("전액 마일리지 결제")
                    .build();
            }
            
            // PG사별 결제 처리
            PgPaymentResult result = switch (payment.getPgProvider()) {
                case "NICE_PAY" -> processNicePay(payment, context);
                case "TOSS_PAYMENTS" -> processTossPayments(payment, context);
                case "KG_INICIS" -> processKgInicis(payment, context);
                case "KAKAO_PAY" -> processKakaoPay(payment, context);
                case "NAVER_PAY" -> processNaverPay(payment, context);
                case "PAYCO" -> processPayco(payment, context);
                // 일반 결제수단은 기본 PG사로 처리
                case "CREDIT_CARD" -> processNicePay(payment, context);  // 신용카드는 나이스페이로 처리
                case "BANK_ACCOUNT" -> processKgInicis(payment, context); // 계좌이체는 이니시스로 처리
                case "BANK_TRANSFER" -> processKgInicis(payment, context); // 무통장입금도 이니시스로 처리
                default -> throw new PaymentExecutionException(
                    "지원하지 않는 PG사입니다: " + payment.getPgProvider());
            };
            
            // 결제 성공 시 PG 정보 업데이트
            if (result.isSuccess()) {
                updatePgInfo(payment, result);
            }
            
            log.info("PG 결제 완료 - success: {}, pgTxId: {}", 
                result.isSuccess(), result.getPgTransactionId());
            
            return result;
            
        } catch (Exception e) {
            log.error("PG 결제 실패 - paymentId: {}", payment.getId(), e);
            throw new PaymentExecutionException("PG 결제 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 나이스페이 결제 처리 (Mock)
     */
    private PgPaymentResult processNicePay(Payment payment, PaymentContext context) {
        log.debug("나이스페이 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 나이스페이 API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("NICE_" + UUID.randomUUID().toString())
            .pgApprovalNo("NICE_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("나이스페이 결제 성공")
            .build();
    }
    
    /**
     * 토스페이먼츠 결제 처리 (Mock)
     */
    private PgPaymentResult processTossPayments(Payment payment, PaymentContext context) {
        log.debug("토스페이먼츠 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 토스페이먼츠 API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("TOSS_" + UUID.randomUUID().toString())
            .pgApprovalNo("TOSS_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("토스페이먼츠 결제 성공")
            .build();
    }
    
    /**
     * KG이니시스 결제 처리 (Mock)
     */
    private PgPaymentResult processKgInicis(Payment payment, PaymentContext context) {
        log.debug("KG이니시스 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 KG이니시스 API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("INICIS_" + UUID.randomUUID().toString())
            .pgApprovalNo("INICIS_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("KG이니시스 결제 성공")
            .build();
    }
    
    /**
     * 카카오페이 결제 처리 (Mock)
     */
    private PgPaymentResult processKakaoPay(Payment payment, PaymentContext context) {
        log.debug("카카오페이 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 카카오페이 API 연동 (MockKakaoPayGateway 사용)
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("KAKAO_" + UUID.randomUUID().toString())
            .pgApprovalNo("KAKAO_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("카카오페이 결제 성공")
            .build();
    }
    
    /**
     * 네이버페이 결제 처리 (Mock)
     */
    private PgPaymentResult processNaverPay(Payment payment, PaymentContext context) {
        log.debug("네이버페이 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 네이버페이 API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("NAVER_" + UUID.randomUUID().toString())
            .pgApprovalNo("NAVER_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("네이버페이 결제 성공")
            .build();
    }
    
    /**
     * PAYCO 결제 처리 (Mock)
     */
    private PgPaymentResult processPayco(Payment payment, PaymentContext context) {
        log.debug("PAYCO 결제 처리 - pgToken: {}", 
            context.getRequest().getPaymentMethod().getPgToken());
        
        // TODO: 실제 PAYCO API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("PAYCO_" + UUID.randomUUID().toString())
            .pgApprovalNo("PAYCO_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("PAYCO 결제 성공")
            .build();
    }
    
    /**
     * Payment 엔티티에 PG 정보 업데이트
     */
    private void updatePgInfo(Payment payment, PgPaymentResult result) {
        payment.updatePgInfo(result.getPgTransactionId(), result.getPgApprovalNo());
        log.debug("PG 정보 업데이트 - pgTxId: {}, pgApproval: {}", 
            result.getPgTransactionId(), result.getPgApprovalNo());
    }
    
    /**
     * PG 결제 취소
     */
    public PgPaymentResult cancelPayment(Payment payment, BigDecimal cancelAmount, String reason) {
        log.info("PG 결제 취소 - paymentId: {}, cancelAmount: {}, reason: {}", 
            payment.getId(), cancelAmount, reason);
        
        // TODO: 실제 PG 취소 API 연동
        // Mock 응답
        return PgPaymentResult.builder()
            .success(true)
            .pgTransactionId("CANCEL_" + payment.getPgTransactionId())
            .pgApprovalNo("CANCEL_APPROVAL_" + System.currentTimeMillis())
            .pgMessage("결제 취소 성공")
            .build();
    }
}