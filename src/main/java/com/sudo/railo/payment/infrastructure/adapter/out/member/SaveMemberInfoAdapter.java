package com.sudo.railo.payment.infrastructure.adapter.out.member;

import com.sudo.railo.payment.application.port.out.SaveMemberInfoPort;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정보 저장 어댑터
 * 
 * SaveMemberInfoPort의 구현체로, 실제 Member 도메인과의 연동을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaveMemberInfoAdapter implements SaveMemberInfoPort {
    
    private final MemberRepository memberRepository;
    
    @Override
    @Transactional
    public void addMileage(Long memberId, Long amount) {
        log.info("마일리지 추가 시작 - memberId: {}, amount: {}", memberId, amount);
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));
        
        member.addMileage(amount);
        memberRepository.save(member);
        
        log.info("마일리지 추가 완료 - memberId: {}, amount: {}, 현재 총 마일리지: {}", 
                memberId, amount, member.getTotalMileage());
    }
    
    @Override
    @Transactional
    public void useMileage(Long memberId, Long amount) {
        log.info("마일리지 사용 시작 - memberId: {}, amount: {}", memberId, amount);
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));
        
        member.useMileage(amount);
        memberRepository.save(member);
        
        log.info("마일리지 사용 완료 - memberId: {}, amount: {}, 현재 총 마일리지: {}", 
                memberId, amount, member.getTotalMileage());
    }
}