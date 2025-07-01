package com.sudo.railo.member.application.util;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.security.util.SecurityUtil;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Member 관련 유틸리티 클래스
 * JWT 토큰에서 현재 로그인한 사용자 정보를 가져오는 기능 제공
 */
@Component
@RequiredArgsConstructor
public class MemberUtil {
    
    private final MemberRepository memberRepository;
    
    /**
     * 현재 로그인한 사용자의 Member 엔티티 조회
     * JWT 토큰에서 memberNo를 추출하여 Member 엔티티 반환
     * 
     * @return 현재 로그인한 Member 엔티티
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public Member getCurrentMember() {
        String currentMemberNo = SecurityUtil.getCurrentMemberNo();
        return memberRepository.findByMemberNo(currentMemberNo)
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
    }
    
    /**
     * 현재 로그인한 사용자의 Member ID 조회
     * JWT 토큰에서 memberNo를 추출하여 Member ID 반환
     * 
     * @return 현재 로그인한 Member의 ID (Primary Key)
     */
    public Long getCurrentMemberId() {
        return getCurrentMember().getId();
    }
    
    /**
     * 현재 로그인한 사용자의 Member 번호 조회
     * JWT 토큰에서 memberNo를 추출하여 반환
     * 
     * @return 현재 로그인한 Member의 memberNo
     */
    public String getCurrentMemberNo() {
        return SecurityUtil.getCurrentMemberNo();
    }
} 