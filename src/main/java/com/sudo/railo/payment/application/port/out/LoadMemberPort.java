package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.member.domain.Member;
import java.util.Optional;

/**
 * 회원 정보 조회 포트
 * 
 * 애플리케이션 계층에서 회원 정보를 조회하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface LoadMemberPort {
    
    /**
     * 회원 타입 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 타입 (GENERAL, VIP 등)
     */
    String getMemberType(Long memberId);
    
    /**
     * 회원 존재 여부 확인
     * 
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    boolean existsById(Long memberId);
    
    /**
     * 회원 엔티티 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 엔티티 (Optional)
     */
    Optional<Member> findById(Long memberId);
}