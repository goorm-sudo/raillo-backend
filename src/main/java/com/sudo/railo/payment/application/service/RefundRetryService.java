package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import com.sudo.railo.payment.domain.repository.RefundCalculationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 환불 재시도 서비스
 * 
 * Unknown 상태의 환불을 주기적으로 확인하고 재시도합니다.
 * 토스/카카오페이 스타일의 실용적 접근법입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundRetryService {
    
    private final RefundCalculationRepository refundCalculationRepository;
    private final RefundService refundService;
    
    /**
     * Unknown 상태 환불 재시도
     * 5분마다 실행되며, 30분 이상 Unknown 상태인 환불은 실패로 처리
     */
    @Scheduled(fixedDelay = 300000) // 5분마다
    @Transactional
    public void retryUnknownRefunds() {
        List<RefundCalculation> unknownRefunds = refundCalculationRepository
                .findByRefundStatus(RefundStatus.UNKNOWN);
        
        if (unknownRefunds.isEmpty()) {
            return;
        }
        
        log.info("Unknown 상태 환불 재시도 시작 - 대상: {}건", unknownRefunds.size());
        
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        
        for (RefundCalculation refund : unknownRefunds) {
            try {
                // 30분 이상 Unknown 상태면 실패로 처리
                if (refund.getRefundRequestTime().isBefore(thirtyMinutesAgo)) {
                    refund.markAsFailed("30분 이상 Unknown 상태 - 자동 실패 처리");
                    refundCalculationRepository.save(refund);
                    log.warn("환불 자동 실패 처리 - refundCalculationId: {}", refund.getId());
                    continue;
                }
                
                // 재시도
                retryRefund(refund);
                
            } catch (Exception e) {
                log.error("환불 재시도 중 오류 - refundCalculationId: {}", refund.getId(), e);
            }
        }
    }
    
    /**
     * 개별 환불 재시도
     */
    private void retryRefund(RefundCalculation refund) {
        log.info("환불 재시도 - refundCalculationId: {}, paymentId: {}", 
                refund.getId(), refund.getPaymentId());
        
        try {
            // RefundService의 processRefund를 호출하여 재시도
            refundService.processRefund(refund.getId());
            log.info("환불 재시도 성공 - refundCalculationId: {}", refund.getId());
            
        } catch (Exception e) {
            // 재시도도 실패하면 여전히 Unknown 상태 유지
            log.warn("환불 재시도 실패 - refundCalculationId: {}, 다음 스케줄에서 재시도", 
                    refund.getId());
        }
    }
    
    /**
     * 수동 재시도 트리거
     * 운영자가 특정 환불을 수동으로 재시도할 때 사용
     */
    @Transactional
    public void manualRetry(Long refundCalculationId) {
        RefundCalculation refund = refundCalculationRepository.findById(refundCalculationId)
                .orElseThrow(() -> new IllegalArgumentException("환불 계산을 찾을 수 없습니다: " + refundCalculationId));
        
        if (refund.getRefundStatus() != RefundStatus.UNKNOWN) {
            throw new IllegalStateException("Unknown 상태가 아닙니다: " + refund.getRefundStatus());
        }
        
        retryRefund(refund);
    }
}