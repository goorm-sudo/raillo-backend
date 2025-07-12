package com.sudo.railo.payment.infrastructure.adapter.out.member;

import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.payment.application.port.out.LoadMemberInfoPort;
import com.sudo.railo.payment.application.port.out.LoadMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 회원 정보 어댑터
 * 
 * 헥사고날 아키텍처의 어댑터로, Member 도메인과의
 * 통신을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class MemberInfoAdapter implements LoadMemberInfoPort {
    
    private final MemberService memberService;
    private final LoadMemberPort loadMemberPort;
    
    @Override
    public BigDecimal getMileageBalance(Long memberId) {
        return memberService.getMileageBalance(memberId);
    }
    
    @Override
    public String getMemberType(Long memberId) {
        // LoadMemberPort를 통해 회원 타입 조회
        return loadMemberPort.getMemberType(memberId);
    }
    
    @Override
    public boolean existsById(Long memberId) {
        return loadMemberPort.existsById(memberId);
    }
}