package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.CalculationStatus;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PaymentCalculation 도메인 리포지토리 인터페이스
 * 결제 계산 세션 관리를 위한 데이터 접근 메서드 정의
 */
public interface PaymentCalculationRepository {
    
    /**
     * 계산 정보 저장
     */
    PaymentCalculation save(PaymentCalculation calculation);
    
    /**
     * 계산 ID로 조회
     */
    Optional<PaymentCalculation> findById(String calculationId);
    
    /**
     * 외부 주문 ID로 조회
     */
    Optional<PaymentCalculation> findByExternalOrderId(String externalOrderId);
    
    /**
     * 사용자 ID로 활성 계산 조회
     */
    List<PaymentCalculation> findByUserIdExternalAndStatus(String userId, CalculationStatus status);
    
    /**
     * 만료된 계산 세션 조회
     */
    List<PaymentCalculation> findByExpiresAtBeforeAndStatus(LocalDateTime expireTime, CalculationStatus status);
    
    /**
     * 계산 상태 일괄 업데이트
     */
    void updateStatusByIds(List<String> calculationIds, CalculationStatus newStatus);
    
    /**
     * 만료된 계산 삭제
     */
    void deleteByExpiresAtBeforeAndStatus(LocalDateTime expireTime, CalculationStatus status);
    
    /**
     * PG 주문번호로 조회
     */
    Optional<PaymentCalculation> findByPgOrderId(String pgOrderId);
} 