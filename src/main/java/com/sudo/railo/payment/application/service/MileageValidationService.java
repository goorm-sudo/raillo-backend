package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 마일리지 검증 서비스
 * 
 * 마일리지 사용에 대한 모든 검증 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MileageValidationService {
    
    private static final BigDecimal MIN_MILEAGE_USE = BigDecimal.valueOf(1000);
    private static final BigDecimal MILEAGE_UNIT = BigDecimal.valueOf(100);
    
    /**
     * 마일리지 사용 검증
     * 
     * @param mileageToUse 사용하려는 마일리지
     * @param availableMileage 사용 가능한 마일리지
     * @throws PaymentValidationException 검증 실패 시
     */
    public void validate(BigDecimal mileageToUse, BigDecimal availableMileage) {
        // null 체크
        if (mileageToUse == null) {
            return; // 마일리지 미사용
        }
        
        // 음수 체크
        if (mileageToUse.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentValidationException("마일리지는 음수일 수 없습니다");
        }
        
        // 0원 체크
        if (mileageToUse.compareTo(BigDecimal.ZERO) == 0) {
            return; // 마일리지 미사용
        }
        
        // 최소 사용 금액 체크
        if (mileageToUse.compareTo(MIN_MILEAGE_USE) < 0) {
            throw new PaymentValidationException(
                String.format("마일리지는 최소 %s포인트 이상 사용해야 합니다", MIN_MILEAGE_USE));
        }
        
        // 사용 단위 체크
        if (mileageToUse.remainder(MILEAGE_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            throw new PaymentValidationException(
                String.format("마일리지는 %s포인트 단위로 사용해야 합니다", MILEAGE_UNIT));
        }
        
        // 보유 마일리지 체크
        if (availableMileage == null) {
            throw new PaymentValidationException("사용 가능한 마일리지 정보가 없습니다");
        }
        
        if (mileageToUse.compareTo(availableMileage) > 0) {
            throw new PaymentValidationException(
                String.format("보유 마일리지가 부족합니다. 보유: %s, 사용 요청: %s", 
                    availableMileage, mileageToUse));
        }
        
        log.info("마일리지 검증 성공 - 사용: {}, 보유: {}", mileageToUse, availableMileage);
    }
    
    /**
     * 결제 금액 대비 마일리지 사용 가능 여부 검증
     * 
     * @param mileageToUse 사용하려는 마일리지
     * @param payableAmount 결제 가능 금액
     */
    public void validateAgainstPayableAmount(BigDecimal mileageToUse, BigDecimal payableAmount) {
        if (mileageToUse == null || mileageToUse.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        
        // 마일리지가 결제 금액보다 큰 경우
        if (mileageToUse.compareTo(payableAmount) > 0) {
            throw new PaymentValidationException(
                String.format("마일리지 사용액이 결제 금액을 초과합니다. 결제금액: %s, 마일리지: %s",
                    payableAmount, mileageToUse));
        }
    }
}