package com.sudo.railo.payment.application.service;

import com.sudo.railo.global.redis.annotation.DistributedLock;
import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.PaymentResult;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import com.sudo.railo.payment.application.mapper.PaymentResponseMapper;
import com.sudo.railo.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스 Facade
 * 
 * 결제 프로세스의 진입점으로 각 전문 서비스를 조율하여
 * 전체 결제 플로우를 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentValidationFacade validationFacade;
    private final PaymentCreationService creationService;
    private final PaymentExecutionService executionService;
    private final PaymentQueryService queryService;
    private final PaymentEventPublisher eventPublisher;
    
    /**
     * 결제 실행 - 단순화된 메인 플로우 (16줄)
     * 
     * @param request 결제 실행 요청
     * @return 결제 실행 응답
     */
    @Transactional
    @DistributedLock(key = "#request.calculationId", prefix = "payment:execute", expireTime = 60)
    public PaymentExecuteResponse executePayment(PaymentExecuteRequest request) {
        log.info("🚀 결제 프로세스 시작 - calculationId: {}, idempotencyKey: {}", request.getId(), request.getIdempotencyKey());
        
        try {
            // 1. 통합 검증 및 컨텍스트 준비
            log.info("📋 1단계: 검증 시작");
            PaymentContext context = validationFacade.validateAndPrepare(request);
            log.info("✅ 1단계: 검증 완료 - 회원타입: {}, 최종금액: {}", 
                context.getMemberType(), context.getFinalPayableAmount());
            
            // 2. Payment 엔티티 생성 및 저장
            log.info("📋 2단계: Payment 엔티티 생성 시작");
            Payment payment = creationService.createPayment(context);
            log.info("✅ 2단계: Payment 엔티티 생성 완료 - paymentId: {}, reservationId: {}", 
                payment.getId(), payment.getReservationId());
            
            // 3. 결제 실행 (마일리지 차감, PG 결제)
            log.info("📋 3단계: 결제 실행 시작");
            PaymentResult result = executionService.execute(payment, context);
            log.info("✅ 3단계: 결제 실행 완료 - success: {}", result.isSuccess());
            
            // 4. 이벤트 발행
            log.info("📋 4단계: 이벤트 발행 시작");
            publishPaymentEvents(result, context);
            log.info("✅ 4단계: 이벤트 발행 완료");
            
            // 5. 응답 생성
            log.info("📋 5단계: 응답 생성");
            PaymentExecuteResponse response = PaymentResponseMapper.from(result, context);
            log.info("🎉 결제 프로세스 완료 - paymentId: {}, status: {}", response.getId(), response.getPaymentStatus());
            
            return response;
            
        } catch (Exception e) {
            log.error("💥 결제 프로세스 실패 - calculationId: {}, 단계: 미상, 예외: {}", 
                request.getId(), e.getClass().getName(), e);
            throw e;
        }
    }
    
    /**
     * 결제 조회
     */
    public PaymentExecuteResponse getPayment(Long paymentId) {
        return queryService.getPayment(paymentId);
    }
    
    /**
     * 결제 이벤트 발행
     * 
     * 이벤트 발행 체계:
     * 1. PaymentExecutionService에서 PaymentStateChangedEvent 발행
     * 2. PaymentEventTranslator가 BookingPaymentCompletedEvent 등으로 변환
     * 3. MileageEventListener가 PaymentStateChangedEvent를 수신하여 마일리지 처리
     * 
     * @deprecated publishPaymentCompleted 제거 - 중복 이벤트 발행 방지
     */
    private void publishPaymentEvents(PaymentResult result, PaymentContext context) {
        Payment payment = result.getPayment();
        
        // publishPaymentCompleted 제거 - PaymentExecutionService에서 이미 PaymentStateChangedEvent 발행
        // 마일리지 처리는 MileageEventListener가 PaymentStateChangedEvent를 수신하여 처리
        
        // 기존 이벤트 발행 (호환성)
        String userId = context.isForMember() ? 
            context.getMemberId().toString() : 
            "NON_MEMBER:" + context.getRequest().getNonMemberName();
            
        eventPublisher.publishExecutionEvent(
            payment.getId().toString(),
            payment.getExternalOrderId(),
            userId
        );
    }
} 