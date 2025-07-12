package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.PaymentResult;
import com.sudo.railo.payment.application.dto.PaymentResult.MileageExecutionResult;
import com.sudo.railo.payment.application.dto.PaymentResult.PgPaymentResult;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.payment.exception.PaymentExecutionException;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;

/**
 * 결제 실행 전용 서비스
 * 
 * Payment 엔티티의 실제 실행(마일리지 차감, PG 결제)을 담당
 * 트랜잭션을 짧게 유지하여 성능 최적화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExecutionService {
    
    private final PaymentRepository paymentRepository;
    private final MileageExecutionService mileageExecutionService;
    private final PgPaymentService pgPaymentService;
    private final PaymentEventPublisher paymentEventPublisher;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 결제 실행
     * 
     * @param payment 생성된 Payment 엔티티
     * @param context 결제 컨텍스트
     * @return 결제 실행 결과
     */
    @Transactional(timeout = 30)
    public PaymentResult execute(Payment payment, PaymentContext context) {
        log.info("결제 실행 시작 - paymentId: {}, amount: {}", 
            payment.getId(), payment.getAmountPaid());
        
        try {
            // 1. 결제 상태를 PROCESSING으로 변경
            PaymentExecutionStatus previousStatus = payment.getPaymentStatus();
            payment.updateStatus(PaymentExecutionStatus.PROCESSING, "결제 처리 시작", "SYSTEM");
            payment = paymentRepository.save(payment);
            
            // 이벤트 발행 (AbstractAggregateRoot 제거로 인해 직접 발행)
            paymentEventPublisher.publishPaymentStateChanged(
                payment, previousStatus, PaymentExecutionStatus.PROCESSING, 
                "결제 처리 시작", "SYSTEM"
            );
            
            // 2. 마일리지 차감 실행 (회원이고 마일리지 사용이 있는 경우)
            MileageExecutionResult mileageResult = null;
            if (context.isForMember() && context.hasMileageUsage()) {
                mileageResult = executeMileageUsage(payment, context);
                log.info("마일리지 차감 완료 - usedPoints: {}, remaining: {}", 
                    mileageResult.getUsedPoints(), mileageResult.getRemainingBalance());
            }
            
            // 3. PG 결제 실행
            PgPaymentResult pgResult = pgPaymentService.processPayment(payment, context);
            if (!pgResult.isSuccess()) {
                throw new PaymentExecutionException("PG 결제 실패: " + pgResult.getPgMessage());
            }
            log.info("PG 결제 완료 - pgTxId: {}", pgResult.getPgTransactionId());
            
            // 4. PG 정보 업데이트
            payment.updatePgInfo(pgResult.getPgTransactionId(), pgResult.getPgApprovalNo());
            
            // 5. 결제 상태를 SUCCESS로 변경
            previousStatus = payment.getPaymentStatus();
            payment.updateStatus(PaymentExecutionStatus.SUCCESS, "결제 완료", "SYSTEM");
            payment = paymentRepository.save(payment);
            entityManager.flush(); // 즉시 DB에 반영
            
            // 이벤트 발행 (AbstractAggregateRoot 제거로 인해 직접 발행)
            log.info("🎯 [이벤트 발행 시작] paymentId: {}, reservationId: {}, {} → {}", 
                payment.getId(), payment.getReservationId(), previousStatus, PaymentExecutionStatus.SUCCESS);
            
            paymentEventPublisher.publishPaymentStateChanged(
                payment, previousStatus, PaymentExecutionStatus.SUCCESS, 
                "결제 완료", "SYSTEM"
            );
            
            log.info("✅ [이벤트 발행 완료] paymentId: {}, reservationId: {}", 
                payment.getId(), payment.getReservationId());
            
            // PaymentStateChangedEvent만 발행 - PaymentEventTranslator가 처리
            // publishPaymentCompleted 제거하여 중복 이벤트 발행 방지
            
            // 5. 성공 결과 반환
            return PaymentResult.success(payment, mileageResult, pgResult);
            
        } catch (Exception e) {
            log.error("결제 실행 실패 - paymentId: {}", payment.getId(), e);
            
            // 6. 실패 처리
            handlePaymentFailure(payment, context, e);
            
            throw new PaymentExecutionException("결제 실행 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 마일리지 차감 실행
     */
    private MileageExecutionResult executeMileageUsage(Payment payment, PaymentContext context) {
        try {
            // 실제 마일리지 차감 실행
            MileageExecutionResult result = mileageExecutionService.executeUsage(payment);
            
            if (!result.isSuccess()) {
                throw new PaymentExecutionException("마일리지 차감 실패");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("마일리지 차감 실패 - paymentId: {}, memberId: {}", 
                payment.getId(), context.getMemberId(), e);
            throw new PaymentExecutionException("마일리지 차감 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 결제 실패 처리
     */
    private void handlePaymentFailure(Payment payment, PaymentContext context, Exception e) {
        try {
            // 1. 결제 상태를 FAILED로 변경
            PaymentExecutionStatus previousStatus = payment.getPaymentStatus();
            payment.updateStatus(PaymentExecutionStatus.FAILED, 
                "결제 실패: " + e.getMessage(), "SYSTEM");
            paymentRepository.save(payment);
            
            // 이벤트 발행 (AbstractAggregateRoot 제거로 인해 직접 발행)
            paymentEventPublisher.publishPaymentStateChanged(
                payment, previousStatus, PaymentExecutionStatus.FAILED, 
                "결제 실패: " + e.getMessage(), "SYSTEM"
            );
            
            // 2. 마일리지 차감이 있었다면 복구
            if (context.isForMember() && context.hasMileageUsage()) {
                try {
                    mileageExecutionService.restoreMileageUsage(
                        context.getCalculation().getId(),
                        context.getMemberId(),
                        context.getMileageResult().getUsageAmount(),
                        String.format("결제 실패로 인한 마일리지 복구 - 결제ID: %s", payment.getId())
                    );
                    log.info("마일리지 복구 완료 - memberId: {}, points: {}", 
                        context.getMemberId(), context.getMileageResult().getUsageAmount());
                } catch (Exception rollbackError) {
                    log.error("마일리지 복구 실패 - memberId: {}", context.getMemberId(), rollbackError);
                    // 마일리지 복구 실패는 별도 처리 필요 (수동 복구 등)
                }
            }
            
        } catch (Exception failureHandlingError) {
            log.error("결제 실패 처리 중 오류", failureHandlingError);
        }
    }
    
    /**
     * 환불 실행 (전체 환불만 지원)
     */
    @Transactional
    public PaymentResult executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        log.info("환불 실행 시작 - paymentId: {}, refundAmount: {}", 
            payment.getId(), refundAmount);
        
        try {
            // 1. 환불 가능 여부 확인
            if (!payment.isRefundable()) {
                throw new PaymentValidationException("환불 불가능한 상태입니다");
            }
            
            // 2. PG 환불 실행
            PgPaymentResult pgResult = pgPaymentService.cancelPayment(payment, refundAmount, reason);
            if (!pgResult.isSuccess()) {
                throw new PaymentExecutionException("PG 환불 실패: " + pgResult.getPgMessage());
            }
            
            // 3. Payment 상태 업데이트
            Payment.RefundRequest refundRequest = Payment.RefundRequest.builder()
                .refundAmount(refundAmount)
                .refundFee(BigDecimal.ZERO) // TODO: 환불 수수료 정책 적용
                .reason(reason)
                .pgTransactionId(pgResult.getPgTransactionId())
                .pgApprovalNo(pgResult.getPgApprovalNo())
                .build();
            
            payment.processRefund(refundRequest);
            payment = paymentRepository.save(payment);
            
            // 4. 마일리지 복구 (전체 환불 시 전액 복구)
            if (payment.getMemberId() != null && 
                payment.getMileagePointsUsed() != null && 
                payment.getMileagePointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    com.sudo.railo.payment.domain.entity.MileageTransaction restoration = mileageExecutionService.restoreMileageUsage(
                        payment.getId().toString(),
                        payment.getMemberId(),
                        payment.getMileagePointsUsed(),
                        String.format("환불로 인한 마일리지 복구 - 결제ID: %s", payment.getId())
                    );
                    log.info("마일리지 복구 완료 - memberId: {}, restoredPoints: {}, transactionId: {}", 
                        payment.getMemberId(), payment.getMileagePointsUsed(), restoration.getId());
                } catch (Exception e) {
                    log.error("마일리지 복구 실패 - 고객센터 문의 필요 - memberId: {}, points: {}", 
                        payment.getMemberId(), payment.getMileagePointsUsed(), e);
                    throw new PaymentExecutionException(
                        "마일리지 복구 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);
                }
            }
            
            return PaymentResult.success(payment, null, pgResult);
            
        } catch (Exception e) {
            log.error("환불 실행 실패 - paymentId: {}", payment.getId(), e);
            throw new PaymentExecutionException("환불 실행 실패: " + e.getMessage(), e);
        }
    }
}