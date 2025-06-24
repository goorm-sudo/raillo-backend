package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.entity.CalculationStatus;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 결제 계산 애플리케이션 서비스
 * 
 * 마일리지 시스템이 완전히 통합된 결제 계산 로직을 제공합니다.
 * - 마일리지 사용 검증 및 계산
 * - 30분 만료 세션 관리
 * - 프로모션 적용 및 스냅샷 저장
 * - 이벤트 기반 아키텍처
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentCalculationService {
    
    private final PaymentCalculationRepository calculationRepository;
    private final PaymentValidationService validationService;
    private final MileageService mileageService;
    private final PaymentEventPublisher eventPublisher;
    
    /**
     * 결제 금액 계산 (마일리지 통합)
     */
    public PaymentCalculationResponse calculatePayment(PaymentCalculationRequest request) {
        log.info("결제 계산 시작 - 주문ID: {}, 사용자: {}, 원본금액: {}, 마일리지사용: {}", 
                request.getExternalOrderId(), request.getUserId(), 
                request.getOriginalAmount(), request.getMileageToUse());
        
        // 1. 기본 검증
        validationService.validateCalculationRequest(request);
        
        // 2. 마일리지 사용 검증
        boolean mileageValid = mileageService.validateMileageUsage(
            request.getMileageToUse(), 
            request.getAvailableMileage(), 
            request.getOriginalAmount()
        );
        
        if (!mileageValid) {
            log.warn("마일리지 사용 검증 실패 - 요청: {}, 보유: {}, 결제금액: {}", 
                    request.getMileageToUse(), request.getAvailableMileage(), request.getOriginalAmount());
            throw new PaymentValidationException("마일리지 사용 조건을 만족하지 않습니다");
        }
        
        // 3. 계산 ID 생성
        String calculationId = UUID.randomUUID().toString();
        
        // 4. 마일리지 할인 적용한 최종 금액 계산
        BigDecimal finalAmount = mileageService.calculateFinalAmount(
            request.getOriginalAmount(), 
            request.getMileageToUse()
        );
        
        // 5. 마일리지 정보 생성
        PaymentCalculationResponse.MileageInfo mileageInfo = buildMileageInfo(request);
        
        // 6. 프로모션 적용 (기존 로직 + 마일리지 통합)
        List<PaymentCalculationResponse.AppliedPromotion> appliedPromotions = 
            applyPromotions(request, finalAmount);
        
        // 7. 계산 결과 저장
        PaymentCalculation calculation = PaymentCalculation.builder()
            .calculationId(calculationId)
            .externalOrderId(request.getExternalOrderId())
            .userIdExternal(request.getUserId())
            .originalAmount(request.getOriginalAmount())
            .finalAmount(finalAmount)
            .promotionSnapshot(serializePromotions(appliedPromotions))
            .status(CalculationStatus.CALCULATED)
            .expiresAt(LocalDateTime.now().plusMinutes(30)) // 30분 후 만료
            .build();
        
        calculationRepository.save(calculation);
        
        // 8. 이벤트 발행
        eventPublisher.publishCalculationEvent(calculationId, request.getExternalOrderId(), request.getUserId());
        
        log.info("결제 계산 완료 - 계산ID: {}, 원본금액: {}, 최종금액: {}, 마일리지할인: {}", 
                calculationId, request.getOriginalAmount(), finalAmount, mileageInfo.getMileageDiscount());
        
        // 9. 응답 생성
        return PaymentCalculationResponse.builder()
            .calculationId(calculationId)
            .externalOrderId(request.getExternalOrderId())
            .originalAmount(request.getOriginalAmount())
            .finalPayableAmount(finalAmount)
            .expiresAt(calculation.getExpiresAt())
            .mileageInfo(mileageInfo)
            .appliedPromotions(appliedPromotions)
            .validationErrors(Collections.emptyList())
            .build();
    }
    
    /**
     * 계산 세션 조회
     */
    public PaymentCalculationResponse getCalculation(String calculationId) {
        PaymentCalculation calculation = calculationRepository.findById(calculationId)
            .orElseThrow(() -> new PaymentValidationException("계산 세션을 찾을 수 없습니다"));
        
        if (calculation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PaymentValidationException("계산 세션이 만료되었습니다");
        }
        
        List<PaymentCalculationResponse.AppliedPromotion> promotions = 
            deserializePromotions(calculation.getPromotionSnapshot());
        
        return PaymentCalculationResponse.builder()
            .calculationId(calculation.getCalculationId())
            .externalOrderId(calculation.getExternalOrderId())
            .originalAmount(calculation.getOriginalAmount())
            .finalPayableAmount(calculation.getFinalAmount())
            .expiresAt(calculation.getExpiresAt())
            .appliedPromotions(promotions)
            .validationErrors(Collections.emptyList())
            .build();
    }
    
    /**
     * 마일리지 정보 생성
     */
    private PaymentCalculationResponse.MileageInfo buildMileageInfo(PaymentCalculationRequest request) {
        BigDecimal usedMileage = request.getMileageToUse();
        BigDecimal availableMileage = request.getAvailableMileage();
        BigDecimal originalAmount = request.getOriginalAmount();
        
        // 마일리지 할인 금액 계산
        BigDecimal mileageDiscount = mileageService.convertMileageToWon(usedMileage);
        
        // 최대 사용 가능 마일리지 계산
        BigDecimal maxUsableMileage = mileageService.calculateMaxUsableAmount(originalAmount);
        
        // 권장 사용 마일리지 계산
        BigDecimal recommendedMileage = mileageService.calculateRecommendedUsage(availableMileage, originalAmount);
        
        // 예상 적립 마일리지 계산 (최종 결제 금액 기준)
        BigDecimal finalAmount = mileageService.calculateFinalAmount(originalAmount, usedMileage);
        BigDecimal expectedEarning = mileageService.calculateEarningAmount(finalAmount);
        
        // 마일리지 사용률 계산
        BigDecimal usageRate = mileageService.calculateUsageRate(usedMileage, originalAmount);
        String usageRateDisplay = String.format("%.1f%%", usageRate.multiply(new BigDecimal("100")));
        
        log.debug("마일리지 정보 생성 - 사용: {}, 할인: {}, 최대사용가능: {}, 권장: {}, 예상적립: {}", 
                usedMileage, mileageDiscount, maxUsableMileage, recommendedMileage, expectedEarning);
        
        return PaymentCalculationResponse.MileageInfo.builder()
            .usedMileage(usedMileage)
            .mileageDiscount(mileageDiscount)
            .availableMileage(availableMileage)
            .maxUsableMileage(maxUsableMileage)
            .recommendedMileage(recommendedMileage)
            .expectedEarning(expectedEarning)
            .usageRate(usageRate)
            .usageRateDisplay(usageRateDisplay)
            .build();
    }
    
    /**
     * 프로모션 적용 (마일리지 통합)
     */
    private List<PaymentCalculationResponse.AppliedPromotion> applyPromotions(
            PaymentCalculationRequest request, BigDecimal finalAmount) {
        
        List<PaymentCalculationResponse.AppliedPromotion> applied = new ArrayList<>();
        
        // 마일리지 사용이 있는 경우 프로모션에 추가
        if (request.getMileageToUse() != null && request.getMileageToUse().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal mileageDiscount = mileageService.convertMileageToWon(request.getMileageToUse());
            
            applied.add(PaymentCalculationResponse.AppliedPromotion.builder()
                .type("MILEAGE")
                .identifier("MILEAGE_USAGE")
                .description(String.format("마일리지 %s포인트 사용", request.getMileageToUse()))
                .pointsUsed(request.getMileageToUse())
                .amountDeducted(mileageDiscount)
                .status("APPLIED")
                .build());
        }
        
        // 기존 프로모션 로직
        if (request.getRequestedPromotions() != null) {
            for (PaymentCalculationRequest.PromotionRequest promotionRequest : request.getRequestedPromotions()) {
                if ("COUPON".equals(promotionRequest.getType())) {
                    // 쿠폰 적용 로직 (향후 구현)
                    applied.add(PaymentCalculationResponse.AppliedPromotion.builder()
                        .type("COUPON")
                        .identifier(promotionRequest.getIdentifier())
                        .description("쿠폰 할인")
                        .status("PENDING")
                        .build());
                }
            }
        }
        
        return applied;
    }
    
    /**
     * 프로모션 JSON 직렬화
     */
    private String serializePromotions(List<PaymentCalculationResponse.AppliedPromotion> promotions) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(promotions);
        } catch (Exception e) {
            log.error("프로모션 직렬화 실패", e);
            return "[]";
        }
    }
    
    /**
     * 프로모션 JSON 역직렬화
     */
    private List<PaymentCalculationResponse.AppliedPromotion> deserializePromotions(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, 
                new TypeReference<List<PaymentCalculationResponse.AppliedPromotion>>() {});
        } catch (Exception e) {
            log.error("프로모션 역직렬화 실패", e);
            return Collections.emptyList();
        }
    }
} 