package com.sudo.railo.payment.infrastructure.adapter.out.persistence;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.payment.application.port.out.LoadMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 회원 영속성 어댑터
 * 
 * 결제 도메인에서 필요한 회원 정보 조회를 담당
 * 회원 도메인의 Repository를 활용하여 필요한 정보만 제공
 */
@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements LoadMemberPort {
    
    private final MemberRepository memberRepository;
    
    @Override
    public String getMemberType(Long memberId) {
        return memberRepository.findById(memberId)
            .map(member -> {
                if (member.getMemberDetail() != null && member.getMemberDetail().getMembership() != null) {
                    // Membership enum을 MemberType 형식으로 변환
                    return switch (member.getMemberDetail().getMembership()) {
                        case VIP, VVIP -> "VIP";
                        case BUSINESS -> "BUSINESS";
                        default -> "GENERAL";
                    };
                }
                return "GENERAL";
            })
            .orElse("GENERAL");
    }
    
    @Override
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }
    
    @Override
    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }
}