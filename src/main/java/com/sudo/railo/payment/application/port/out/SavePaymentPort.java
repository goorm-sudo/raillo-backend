package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.Payment;

/**
 * 결제 정보 저장 포트
 * 
 * 애플리케이션 계층에서 결제 정보를 저장하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface SavePaymentPort {
    
    /**
     * 결제 정보 저장
     * 
     * @param payment 결제 엔티티
     * @return 저장된 결제 엔티티
     */
    Payment save(Payment payment);
}