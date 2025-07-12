package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.event.PaymentCancelledEvent;
import com.sudo.railo.payment.application.event.PaymentRefundedEvent;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.exception.PaymentException;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentService;
import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentCancelRequest;
import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentCancelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불/취소 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundService {
    
    private final PaymentRepository paymentRepository;
    private final PgPaymentService pgPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 결제 취소 처리 (결제 전 취소)
     */
    @Transactional
    public void cancelPayment(Long paymentId, String reason) {
        log.debug("결제 취소 처리 시작 - paymentId: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentValidationException("존재하지 않는 결제입니다: " + paymentId));
        
        // 취소 가능 여부 확인
        if (!payment.isCancellable()) {
            throw new PaymentValidationException("취소 불가능한 결제 상태입니다: " + payment.getPaymentStatus());
        }
        
        try {
            // PG사 취소 처리 (결제 전이므로 실제로는 요청만 취소)
            if (payment.getPgTransactionId() != null) {
                PgPaymentCancelRequest cancelRequest = PgPaymentCancelRequest.builder()
                        .pgTransactionId(payment.getPgTransactionId())
                        .merchantOrderId(payment.getExternalOrderId())
                        .cancelAmount(payment.getAmountPaid())
                        .cancelReason(reason)
                        .build();
                
                PgPaymentCancelResponse cancelResponse = pgPaymentService.cancelPayment(payment.getPaymentMethod(), cancelRequest);
                
                if (!cancelResponse.isSuccess()) {
                    throw new PaymentException("PG사 취소 처리 실패: " + cancelResponse.getErrorMessage());
                }
            }
            
            // Payment 상태 업데이트
            payment.cancel(reason);
            paymentRepository.save(payment);
            
            // 취소 이벤트 발행 (마일리지 복구용)
            PaymentCancelledEvent cancelledEvent = PaymentCancelledEvent.builder()
                    .paymentId(payment.getId())
                    .reservationId(payment.getReservationId())
                    .externalOrderId(payment.getExternalOrderId())
                    .memberId(payment.getMemberId())
                    .cancelReason(reason)
                    .cancelledAt(payment.getCancelledAt())
                    .mileageToRestore(payment.getMileagePointsUsed())
                    .mileageEarnedToCancel(payment.getMileageToEarn())
                    .build();
            
            eventPublisher.publishEvent(cancelledEvent);
            
            log.debug("결제 취소 처리 완료 - paymentId: {}", paymentId);
            
        } catch (Exception e) {
            log.error("결제 취소 처리 중 오류 발생 - paymentId: {}", paymentId, e);
            throw new PaymentException("결제 취소 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 결제 환불 처리 (전체 환불)
     */
    @Transactional
    public void refundPayment(Long paymentId, String reason) {
        log.debug("결제 환불 처리 시작 - paymentId: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentValidationException("존재하지 않는 결제입니다: " + paymentId));
        
        // 환불 가능 여부 확인
        if (!payment.isRefundable()) {
            throw new PaymentValidationException("환불 불가능한 결제 상태입니다: " + payment.getPaymentStatus());
        }
        
        // 이미 환불된 결제인지 확인
        if (payment.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new PaymentValidationException("이미 환불 처리된 결제입니다");
        }
        
        BigDecimal refundAmount = payment.getAmountPaid(); // 전체 환불
        
        try {
            // PG사 환불 처리
            PgPaymentCancelRequest pgCancelRequest = PgPaymentCancelRequest.builder()
                    .pgTransactionId(payment.getPgTransactionId())
                    .merchantOrderId(payment.getExternalOrderId())
                    .cancelAmount(refundAmount)
                    .cancelReason(reason)
                    .build();
            
            PgPaymentCancelResponse refundResponse = pgPaymentService.cancelPayment(payment.getPaymentMethod(), pgCancelRequest);
            
            if (!refundResponse.isSuccess()) {
                throw new PaymentException("PG사 환불 처리 실패: " + refundResponse.getErrorMessage());
            }
            
            // 환불 수수료 계산
            BigDecimal refundFee = calculateRefundFee(payment, refundAmount);
            
            // Payment 상태 업데이트 (전체 환불)
            Payment.RefundRequest refundRequest = Payment.RefundRequest.builder()
                .refundAmount(refundAmount)
                .refundFee(refundFee)
                .reason(reason)
                .pgTransactionId(refundResponse.getPgTransactionId())
                .pgApprovalNo(refundResponse.getCancelApprovalNumber())
                .build();
            payment.processRefund(refundRequest);
            
            paymentRepository.save(payment);
            
            // 환불 이벤트 발행
            PaymentRefundedEvent refundedEvent = PaymentRefundedEvent.builder()
                    .paymentId(payment.getId())
                    .reservationId(payment.getReservationId())
                    .externalOrderId(payment.getExternalOrderId())
                    .memberId(payment.getMemberId())
                    .refundAmount(refundAmount)
                    .refundFee(refundFee)
                    .refundReason(reason)
                    .pgRefundTransactionId(refundResponse.getPgTransactionId())
                    .refundedAt(payment.getRefundedAt())
                    .mileageToRestore(payment.getMileagePointsUsed()) // 사용한 마일리지 전체 복구
                    .mileageEarnedToCancel(payment.getMileageToEarn()) // 적립 예정 마일리지 전체 취소
                    .build();
            
            eventPublisher.publishEvent(refundedEvent);
            
            log.debug("결제 환불 처리 완료 - paymentId: {}, amount: {}", paymentId, refundAmount);
            
        } catch (Exception e) {
            log.error("결제 환불 처리 중 오류 발생 - paymentId: {}", paymentId, e);
            throw new PaymentException("결제 환불 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 환불 수수료 계산
     */
    private BigDecimal calculateRefundFee(Payment payment, BigDecimal refundAmount) {
        // 결제일로부터 24시간 이내는 무료, 이후는 환불 금액의 1% (최소 1000원)
        LocalDateTime paymentTime = payment.getPaidAt();
        LocalDateTime now = LocalDateTime.now();
        
        if (paymentTime != null && paymentTime.plusDays(1).isAfter(now)) {
            return BigDecimal.ZERO; // 24시간 이내 무료
        }
        
        BigDecimal feeRate = new BigDecimal("0.01"); // 1%
        BigDecimal calculatedFee = refundAmount.multiply(feeRate);
        BigDecimal minFee = new BigDecimal("1000");
        
        return calculatedFee.max(minFee);
    }
}