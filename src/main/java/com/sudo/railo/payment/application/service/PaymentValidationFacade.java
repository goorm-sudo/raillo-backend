package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.MileageValidationResult;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MemberTypeService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 검증 통합 서비스
 * 
 * 결제 실행에 필요한 모든 검증을 통합하여 처리하고
 * PaymentContext를 생성하여 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentValidationFacade {
    
    private final PaymentValidationService validationService;
    private final PaymentCalculationService calculationService;
    private final MileageService mileageService;
    private final MemberTypeService memberTypeService;
    
    /**
     * 결제 요청 검증 및 컨텍스트 준비
     * 
     * @param request 결제 실행 요청
     * @return 검증된 결제 컨텍스트
     */
    @Transactional(readOnly = true)
    public PaymentContext validateAndPrepare(PaymentExecuteRequest request) {
        log.info("결제 검증 시작 - calculationId: {}", request.getId());
        
        try {
            // 1. 기본 요청 검증
            validationService.validateExecuteRequest(request);
            log.debug("기본 요청 검증 완료");
            
            // 2. 회원/비회원 타입 판별
            MemberType memberType = memberTypeService.determineMemberType(request);
            log.info("회원 타입 확인 - {}", memberType.getDescription());
            
            // 3. 계산 세션 조회 및 검증
            PaymentCalculationResponse calculation = calculationService.getCalculation(
                request.getId()
            );
            validateCalculation(calculation, request);
            log.debug("계산 세션 검증 완료 - 예약ID: {}", calculation.getReservationId());
            
            // 4. 마일리지 검증 (회원인 경우만)
            MileageValidationResult mileageResult = validateMileage(request, calculation, memberType);
            if (mileageResult.hasMileageUsage()) {
                log.info("마일리지 사용 검증 완료 - 사용포인트: {}, 차감금액: {}", 
                    mileageResult.getUsageAmount(), mileageResult.getDeductionAmount());
            }
            
            // 5. 최종 금액 검증
            validateFinalAmount(request, calculation, mileageResult);
            
            // 6. PaymentContext 생성
            PaymentContext context = PaymentContext.builder()
                .request(request)
                .calculation(calculation)
                .mileageResult(convertMileageResult(mileageResult))
                .memberType(memberType)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            
            log.info("결제 검증 완료 - 최종금액: {}", 
                calculation.getFinalPayableAmount().subtract(
                    mileageResult.getDeductionAmount() != null ? 
                    mileageResult.getDeductionAmount() : BigDecimal.ZERO
                ));
            
            return context;
            
        } catch (PaymentValidationException e) {
            log.error("결제 검증 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 검증 중 예외 발생", e);
            throw new PaymentValidationException("결제 검증 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 계산 세션 검증
     */
    private void validateCalculation(PaymentCalculationResponse calculation, 
                                   PaymentExecuteRequest request) {
        // 계산 ID 일치 확인
        if (!calculation.getId().equals(request.getId())) {
            throw new PaymentValidationException("계산 ID가 일치하지 않습니다");
        }
        
        // 만료 시간 확인
        if (calculation.isExpired()) {
            throw new PaymentValidationException("계산 세션이 만료되었습니다");
        }
        
        // 최소 결제 금액 확인
        if (calculation.getFinalPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("결제 금액이 0원 이하입니다");
        }
    }
    
    /**
     * 마일리지 검증
     */
    private MileageValidationResult validateMileage(PaymentExecuteRequest request,
                                                   PaymentCalculationResponse calculation,
                                                   MemberType memberType) {
        // 비회원은 마일리지 사용 불가
        if (memberType != MemberType.MEMBER) {
            return MileageValidationResult.notUsed();
        }
        
        // 마일리지 사용 요청이 없는 경우
        BigDecimal mileageToUse = request.getMileageToUse();
        if (mileageToUse == null || mileageToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return MileageValidationResult.notUsed();
        }
        
        try {
            // 마일리지 사용 가능 여부 검증
            mileageService.validateMileageUsage(
                mileageToUse,
                request.getAvailableMileage(),
                calculation.getFinalPayableAmount()
            );
            
            // 마일리지 -> 원화 변환
            BigDecimal deductionAmount = mileageService.convertMileageToWon(mileageToUse);
            
            return MileageValidationResult.success(
                mileageToUse,
                request.getAvailableMileage(),
                deductionAmount
            );
            
        } catch (PaymentValidationException e) {
            log.warn("마일리지 검증 실패 - {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 최종 결제 금액 검증
     */
    private void validateFinalAmount(PaymentExecuteRequest request,
                                   PaymentCalculationResponse calculation,
                                   MileageValidationResult mileageResult) {
        BigDecimal calculatedAmount = calculation.getFinalPayableAmount();
        BigDecimal mileageDeduction = mileageResult.getDeductionAmount() != null ? 
            mileageResult.getDeductionAmount() : BigDecimal.ZERO;
        
        BigDecimal finalAmount = calculatedAmount.subtract(mileageDeduction);
        
        // 최종 금액이 음수인지 확인
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentValidationException(
                String.format("마일리지 차감 후 결제 금액이 음수입니다. " +
                    "결제금액: %s, 마일리지차감: %s", 
                    calculatedAmount, mileageDeduction)
            );
        }
        
        // 전액 마일리지 결제인 경우 PG 결제 정보 불필요
        if (finalAmount.compareTo(BigDecimal.ZERO) == 0 && 
            request.getPaymentMethod().getPgToken() != null) {
            log.warn("전액 마일리지 결제에 PG 토큰이 포함되어 있습니다");
        }
    }
    
    /**
     * MileageValidationResult 타입 변환
     * dto → context 내부 클래스로 변환
     */
    private PaymentContext.MileageValidationResult convertMileageResult(
            MileageValidationResult dtoResult) {
        if (dtoResult == null || !dtoResult.isValid()) {
            return PaymentContext.MileageValidationResult.failure(
                dtoResult != null ? dtoResult.getFailureReason() : "마일리지 검증 실패"
            );
        }
        
        return PaymentContext.MileageValidationResult.success(
            dtoResult.getAvailableBalance(),
            dtoResult.getUsageAmount()
        );
    }
}