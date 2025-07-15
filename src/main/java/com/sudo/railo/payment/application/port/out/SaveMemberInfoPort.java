package com.sudo.railo.payment.application.port.out;

/**
 * 회원 정보 저장 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, Member 도메인에서
 * 마일리지 관련 정보를 업데이트하는 기능을 정의합니다.
 */
public interface SaveMemberInfoPort {
    
    /**
     * 회원의 마일리지를 추가합니다.
     * 
     * @param memberId 회원 ID
     * @param amount 추가할 마일리지 금액
     */
    void addMileage(Long memberId, Long amount);
    
    /**
     * 회원의 마일리지를 차감합니다.
     * 
     * @param memberId 회원 ID
     * @param amount 차감할 마일리지 금액
     */
    void useMileage(Long memberId, Long amount);
}