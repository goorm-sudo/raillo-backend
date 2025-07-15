package com.sudo.railo.payment.application.port.out;

import java.math.BigDecimal;

/**
 * 회원 정보 조회 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, Member 도메인에서
 * 마일리지 적립에 필요한 회원 정보를 조회하는 기능을 정의합니다.
 */
public interface LoadMemberInfoPort {
    
    /**
     * 회원의 현재 마일리지 잔액을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @return 현재 마일리지 잔액
     */
    BigDecimal getMileageBalance(Long memberId);
    
    /**
     * 회원 타입을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @return 회원 타입 (VIP, BUSINESS, GENERAL 등)
     */
    String getMemberType(Long memberId);
    
    /**
     * 회원 존재 여부를 확인합니다.
     * 
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    boolean existsById(Long memberId);
}