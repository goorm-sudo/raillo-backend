package com.sudo.railo.member.adapter.out.persistence;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.payment.application.port.out.SaveMemberInfoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 마일리지 정보 저장 어댑터
 * Payment 모듈에서 Member 모듈의 마일리지를 업데이트하기 위한 구현체
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaveMemberInfoAdapter implements SaveMemberInfoPort {
    
    private final MemberRepository memberRepository;
    
    @Override
    @Transactional
    public void addMileage(Long memberId, Long amount) {
        log.debug("회원 마일리지 추가 - 회원ID: {}, 금액: {}", memberId, amount);
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. ID: " + memberId));
        
        member.addMileage(amount);
        memberRepository.save(member);
        
        log.info("회원 마일리지 추가 완료 - 회원ID: {}, 추가금액: {}, 현재잔액: {}", 
                memberId, amount, member.getTotalMileage());
    }
    
    @Override
    @Transactional
    public void useMileage(Long memberId, Long amount) {
        log.debug("회원 마일리지 차감 - 회원ID: {}, 금액: {}", memberId, amount);
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. ID: " + memberId));
        
        member.useMileage(amount);
        memberRepository.save(member);
        
        log.info("회원 마일리지 차감 완료 - 회원ID: {}, 차감금액: {}, 현재잔액: {}", 
                memberId, amount, member.getTotalMileage());
    }
}