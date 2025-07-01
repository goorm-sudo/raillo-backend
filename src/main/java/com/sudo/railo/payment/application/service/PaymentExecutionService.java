package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MemberTypeService;
import com.sudo.railo.payment.domain.service.NonMemberService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 실행 전용 서비스
 * Command 패턴 적용 - 결제 실행만 담당
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentExecutionService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final PaymentCalculationService calculationService;
    private final PaymentEventPublisher eventPublisher;
    private final MemberTypeService memberTypeService;
    private final NonMemberService nonMemberService;
    private final MileageService mileageService;
    private final MileageExecutionService mileageExecutionService;
    
    public PaymentExecuteResponse execute(PaymentExecuteRequest request) {
        // 1. 기본 검증
        validationService.validateExecuteRequest(request);
        
        // 2. 회원/비회원 타입 판별
        MemberType memberType = memberTypeService.determineMemberType(request);
        log.info("결제 요청 타입 확인 - 타입: {}", memberType.getDescription());
        
        // 3. 계산 세션 검증
        PaymentCalculationResponse calculation = calculationService.getCalculation(request.getCalculationId());
        
        // 4. 중복 결제 체크
        validateIdempotency(request.getIdempotencyKey());
        
        // 5. 마일리지 처리
        MileageProcessResult mileageResult = processMileage(request, calculation, memberType);
        
        // 6. Payment 엔티티 생성
        Payment payment = buildPayment(request, calculation, mileageResult, memberType);
        
        // 7. 비회원 정보 저장
        if (memberType == MemberType.NON_MEMBER) {
            nonMemberService.saveNonMemberInfo(payment, request);
        }
        
        try {
            // 8. 결제 실행 및 저장
            Payment savedPayment = executePaymentTransaction(payment, mileageResult, memberType, request);
            
            // 9. 이벤트 발행
            publishEvents(savedPayment, memberType, request);
            
            // 10. 응답 생성
            return buildResponse(savedPayment, memberType, mileageResult);
            
        } catch (Exception e) {
            handlePaymentFailure(request, mileageResult, memberType, e);
            throw new PaymentValidationException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private void validateIdempotency(String idempotencyKey) {
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new PaymentValidationException("이미 처리된 결제 요청입니다");
        }
    }
    
    private MileageProcessResult processMileage(PaymentExecuteRequest request, 
                                               PaymentCalculationResponse calculation, 
                                               MemberType memberType) {
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
        
        // 마일리지 적립 포인트 계산 (회원인 경우만)
        if (memberType == MemberType.MEMBER) {
            mileageToEarn = mileageService.calculateEarningAmount(calculation.getFinalPayableAmount());
            log.info("마일리지 적립 예정 - 포인트: {}", mileageToEarn);
        }
        
        return new MileageProcessResult(mileagePointsUsed, mileageAmountDeducted, mileageToEarn);
    }
    
    private Payment buildPayment(PaymentExecuteRequest request, 
                                PaymentCalculationResponse calculation,
                                MileageProcessResult mileageResult, 
                                MemberType memberType) {
        
        // reservationId는 calculation.getReservationId()에서 가져오고, 
        // 만약 String 형태라면 숫자 부분만 추출하여 사용
        Long reservationId;
        try {
            // reservationId가 "R2025060100001" 형태라면 숫자 부분만 추출
            String reservationIdStr = calculation.getReservationId();
            if (reservationIdStr.startsWith("R")) {
                // "R" 제거 후 숫자 부분만 파싱
                reservationId = Long.parseLong(reservationIdStr.substring(1));
            } else {
                // 이미 숫자 형태라면 그대로 파싱
                reservationId = Long.parseLong(reservationIdStr);
            }
        } catch (NumberFormatException e) {
            log.error("reservationId 파싱 실패: {}", calculation.getReservationId(), e);
            throw new PaymentValidationException("잘못된 예약 ID 형식입니다: " + calculation.getReservationId());
        }
        
        Payment.PaymentBuilder paymentBuilder = Payment.builder()
            .reservationId(reservationId)
            .externalOrderId(calculation.getExternalOrderId())
            .amountOriginalTotal(calculation.getOriginalAmount())
            .amountPaid(calculation.getFinalPayableAmount().subtract(mileageResult.amountDeducted))
            .mileagePointsUsed(mileageResult.pointsUsed)
            .mileageAmountDeducted(mileageResult.amountDeducted)
            .mileageToEarn(mileageResult.toEarn)
            .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().getType()))
            .pgProvider(request.getPaymentMethod().getPgProvider())
            .paymentStatus(PaymentExecutionStatus.PROCESSING)
            .idempotencyKey(request.getIdempotencyKey());
        
        // 회원/비회원별 추가 정보 설정
        if (memberType == MemberType.MEMBER) {
            paymentBuilder.memberId(request.getMemberId());
            log.info("회원 결제 처리 - 회원ID: {}", request.getMemberId());
        } else {
            paymentBuilder.memberId(null);
            log.info("비회원 결제 처리 - 이름: {}", request.getNonMemberName());
        }
        
        return paymentBuilder.build();
    }
    
    private Payment executePaymentTransaction(Payment payment, 
                                            MileageProcessResult mileageResult,
                                            MemberType memberType, 
                                            PaymentExecuteRequest request) {
        // 실제 마일리지 차감 실행 (회원인 경우만)
        if (memberType == MemberType.MEMBER && mileageResult.pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            try {
                mileageExecutionService.executeUsage(payment);
                log.info("마일리지 차감 완료 - 회원ID: {}, 차감포인트: {}", request.getMemberId(), mileageResult.pointsUsed);
            } catch (Exception e) {
                log.error("마일리지 차감 실패 - 회원ID: {}, 차감포인트: {}", request.getMemberId(), mileageResult.pointsUsed, e);
                throw new PaymentValidationException("마일리지 차감 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        // 결제 상태를 완료로 변경 (실제로는 PG 연동 후)
        payment.updateStatus(PaymentExecutionStatus.SUCCESS);
        
        // 저장
        return paymentRepository.save(payment);
    }
    
    private void publishEvents(Payment savedPayment, MemberType memberType, PaymentExecuteRequest request) {
        // 결제 완료 이벤트 발행 (마일리지 적립 트리거)
        if (memberType == MemberType.MEMBER) {
            eventPublisher.publishPaymentCompleted(savedPayment);
            log.info("결제 완료 이벤트 발행 - 결제ID: {}, 적립예정: {}", 
                    savedPayment.getPaymentId(), savedPayment.getMileageToEarn());
        }
        
        // 기존 이벤트도 발행 (호환성)
        String userId = memberType == MemberType.MEMBER ? 
                request.getMemberId().toString() : "NON_MEMBER:" + request.getNonMemberName();
        eventPublisher.publishExecutionEvent(savedPayment.getPaymentId().toString(), 
            savedPayment.getExternalOrderId(), userId);
    }
    
    private PaymentExecuteResponse buildResponse(Payment savedPayment, 
                                               MemberType memberType,
                                               MileageProcessResult mileageResult) {
        return PaymentExecuteResponse.builder()
            .paymentId(savedPayment.getPaymentId())
            .externalOrderId(savedPayment.getExternalOrderId())
            .paymentStatus(PaymentExecutionStatus.SUCCESS)
            .amountPaid(savedPayment.getAmountPaid())
            .mileagePointsUsed(savedPayment.getMileagePointsUsed())
            .mileageAmountDeducted(savedPayment.getMileageAmountDeducted())
            .mileageToEarn(savedPayment.getMileageToEarn())
            .result(PaymentExecuteResponse.PaymentResult.builder()
                .success(true)
                .message(memberType == MemberType.MEMBER ? 
                        "회원 결제가 완료되었습니다. 마일리지 " + mileageResult.toEarn + "포인트가 적립됩니다." : 
                        "비회원 결제가 완료되었습니다.")
                .build())
            .build();
    }
    
    private void handlePaymentFailure(PaymentExecuteRequest request,
                                    MileageProcessResult mileageResult, 
                                    MemberType memberType, 
                                    Exception e) {
        log.error("결제 처리 중 오류 발생 - 계산ID: {}, 오류: {}", request.getCalculationId(), e.getMessage());
        
        // 마일리지 차감이 있었다면 복구
        if (memberType == MemberType.MEMBER && mileageResult.pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            try {
                mileageExecutionService.restoreUsage(
                    request.getCalculationId(), 
                    request.getMemberId(), 
                    mileageResult.pointsUsed
                );
                log.info("마일리지 차감 롤백 완료 - 회원ID: {}, 복구포인트: {}", 
                        request.getMemberId(), mileageResult.pointsUsed);
            } catch (Exception rollbackError) {
                log.error("마일리지 롤백 실패 - 회원ID: {}, 복구포인트: {}", 
                        request.getMemberId(), mileageResult.pointsUsed, rollbackError);
                // 롤백 실패해도 원본 예외를 유지
            }
        }
    }
    
    /**
     * 마일리지 처리 결과 VO
     */
    private record MileageProcessResult(
        BigDecimal pointsUsed,
        BigDecimal amountDeducted,
        BigDecimal toEarn
    ) {}
} 