package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentStatus;
import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MemberTypeService;
import com.sudo.railo.payment.domain.service.NonMemberService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final PaymentCalculationService calculationService;
    private final PaymentEventPublisher eventPublisher;
    private final MemberTypeService memberTypeService;
    private final NonMemberService nonMemberService;
    private final MileageService mileageService;
    
    public PaymentExecuteResponse executePayment(PaymentExecuteRequest request) {
        
        // 1. 기본 검증
        validationService.validateExecuteRequest(request);
        
        // 2. 회원/비회원 타입 판별
        MemberType memberType = memberTypeService.determineMemberType(request);
        log.info("결제 요청 타입 확인 - 타입: {}", memberType.getDescription());
        
        // 3. 계산 세션 검증
        PaymentCalculationResponse calculation = calculationService.getCalculation(request.getCalculationId());
        
        // 4. 중복 결제 체크
        if (paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new PaymentValidationException("이미 처리된 결제 요청입니다");
        }
        
        // 5. 마일리지 사용 검증 및 차감 준비 (회원인 경우만)
        BigDecimal mileagePointsUsed = BigDecimal.ZERO;
        BigDecimal mileageAmountDeducted = BigDecimal.ZERO;
        BigDecimal mileageToEarn = BigDecimal.ZERO;
        
        if (memberType == MemberType.MEMBER && request.getMileageToUse() != null && 
            request.getMileageToUse().compareTo(BigDecimal.ZERO) > 0) {
            
            // 마일리지 사용 검증
            mileageService.validateMileageUsage(
                request.getMileageToUse(), 
                request.getAvailableMileage(), 
                calculation.getFinalPayableAmount()
            );
            
            mileagePointsUsed = request.getMileageToUse();
            mileageAmountDeducted = mileageService.convertMileageToWon(mileagePointsUsed);
            
            log.info("마일리지 사용 - 포인트: {}, 차감금액: {}", mileagePointsUsed, mileageAmountDeducted);
        }
        
        // 6. 마일리지 적립 포인트 계산 (회원인 경우만)
        if (memberType == MemberType.MEMBER) {
            mileageToEarn = mileageService.calculateEarningAmount(calculation.getFinalPayableAmount());
            log.info("마일리지 적립 예정 - 포인트: {}", mileageToEarn);
        }
        
        // 7. Payment 엔티티 생성 (마일리지 정보 포함)
        Payment.PaymentBuilder paymentBuilder = Payment.builder()
            .reservationId(calculation.getReservationId())
            .externalOrderId(calculation.getExternalOrderId())
            .amountOriginalTotal(calculation.getOriginalAmount())
            .amountPaid(calculation.getFinalPayableAmount().subtract(mileageAmountDeducted))
            .mileagePointsUsed(mileagePointsUsed)
            .mileageAmountDeducted(mileageAmountDeducted)
            .mileageToEarn(mileageToEarn)
            .paymentMethod(request.getPaymentMethod().getType())
            .pgProvider(request.getPaymentMethod().getPgProvider())
            .paymentStatus(PaymentStatus.PROCESSING)
            .idempotencyKey(request.getIdempotencyKey());
        
        // 8. 회원/비회원별 추가 정보 설정
        if (memberType == MemberType.MEMBER) {
            paymentBuilder.memberId(request.getMemberId());
            log.info("회원 결제 처리 - 회원ID: {}", request.getMemberId());
        } else {
            // 비회원인 경우 null로 설정
            paymentBuilder.memberId(null);
            log.info("비회원 결제 처리 - 이름: {}", request.getNonMemberName());
        }
        
        Payment payment = paymentBuilder.build();
        
        // 9. 비회원 정보 저장 (암호화 포함)
        if (memberType == MemberType.NON_MEMBER) {
            nonMemberService.saveNonMemberInfo(payment, request);
        }
        
        try {
            // 10. 실제 마일리지 차감 실행 (회원인 경우만)
            if (memberType == MemberType.MEMBER && mileagePointsUsed.compareTo(BigDecimal.ZERO) > 0) {
                // TODO: 실제 마일리지 차감 로직 구현 (MileageExecutionService)
                log.info("마일리지 차감 실행 - 회원ID: {}, 차감포인트: {}", request.getMemberId(), mileagePointsUsed);
            }
            
            // 11. 결제 상태를 완료로 변경 (실제로는 PG 연동 후)
            payment.updateStatus(PaymentStatus.SUCCESS);
            
            // 12. 저장
            Payment savedPayment = paymentRepository.save(payment);
            
            // 13. 결제 완료 이벤트 발행 (마일리지 적립 트리거)
            if (memberType == MemberType.MEMBER) {
                eventPublisher.publishPaymentCompleted(
                    savedPayment.getPaymentId().toString(),
                    savedPayment.getMemberId(),
                    savedPayment.getAmountPaid(),
                    savedPayment.getMileageToEarn()
                );
                log.info("결제 완료 이벤트 발행 - 결제ID: {}, 적립예정: {}", 
                        savedPayment.getPaymentId(), savedPayment.getMileageToEarn());
            }
            
            // 14. 기존 이벤트도 발행 (호환성)
            String userId = memberType == MemberType.MEMBER ? 
                    request.getMemberId().toString() : "NON_MEMBER:" + request.getNonMemberName();
            eventPublisher.publishExecutionEvent(savedPayment.getPaymentId().toString(), 
                savedPayment.getExternalOrderId(), userId);
            
            // 15. 응답 생성
            return PaymentExecuteResponse.builder()
                .paymentId(savedPayment.getPaymentId())
                .externalOrderId(savedPayment.getExternalOrderId())
                .paymentStatus(PaymentStatus.SUCCESS)
                .amountPaid(savedPayment.getAmountPaid())
                .mileagePointsUsed(savedPayment.getMileagePointsUsed())
                .mileageAmountDeducted(savedPayment.getMileageAmountDeducted())
                .mileageToEarn(savedPayment.getMileageToEarn())
                .result(PaymentExecuteResponse.PaymentResult.builder()
                    .success(true)
                    .message(memberType == MemberType.MEMBER ? 
                            "회원 결제가 완료되었습니다. 마일리지 " + mileageToEarn + "포인트가 적립됩니다." : 
                            "비회원 결제가 완료되었습니다.")
                    .build())
                .build();
                
        } catch (Exception e) {
            // 16. 예외 발생 시 롤백 처리
            log.error("결제 처리 중 오류 발생 - 계산ID: {}, 오류: {}", request.getCalculationId(), e.getMessage());
            
            // 마일리지 차감이 있었다면 복구 (TODO: 실제 구현 시)
            if (memberType == MemberType.MEMBER && mileagePointsUsed.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("마일리지 차감 롤백 필요 - 회원ID: {}, 복구포인트: {}", 
                        request.getMemberId(), mileagePointsUsed);
            }
            
            throw new PaymentValidationException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    public PaymentExecuteResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentValidationException("결제 정보를 찾을 수 없습니다"));
        
        return PaymentExecuteResponse.builder()
            .paymentId(payment.getPaymentId())
            .externalOrderId(payment.getExternalOrderId())
            .paymentStatus(payment.getPaymentStatus())
            .amountPaid(payment.getAmountPaid())
            .mileagePointsUsed(payment.getMileagePointsUsed())
            .mileageAmountDeducted(payment.getMileageAmountDeducted())
            .mileageToEarn(payment.getMileageToEarn())
            .pgTransactionId(payment.getPgTransactionId())
            .pgApprovalNo(payment.getPgApprovalNo())
            .receiptUrl(payment.getReceiptUrl())
            .paidAt(payment.getPaidAt())
            .build();
    }
} 