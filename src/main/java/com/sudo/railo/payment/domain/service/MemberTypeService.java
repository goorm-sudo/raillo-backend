package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 회원 타입 판별 도메인 서비스
 */
@Service
public class MemberTypeService {
    
    /**
     * 결제 요청으로부터 회원 타입을 판별
     */
    public MemberType determineMemberType(PaymentExecuteRequest request) {
        if (request.getMemberId() != null && request.getMemberId() > 0) {
            return MemberType.MEMBER;
        }
        
        if (hasNonMemberInfo(request)) {
            return MemberType.NON_MEMBER;
        }
        
        throw new IllegalArgumentException("회원 ID 또는 비회원 정보가 필요합니다");
    }
    
    /**
     * 비회원 정보 존재 여부 확인
     */
    private boolean hasNonMemberInfo(PaymentExecuteRequest request) {
        return StringUtils.hasText(request.getNonMemberName()) 
            && StringUtils.hasText(request.getNonMemberPhone())
            && StringUtils.hasText(request.getNonMemberPassword());
    }
    
    /**
     * 회원 여부 확인
     */
    public boolean isMember(PaymentExecuteRequest request) {
        return determineMemberType(request) == MemberType.MEMBER;
    }
    
    /**
     * 비회원 여부 확인
     */
    public boolean isNonMember(PaymentExecuteRequest request) {
        return determineMemberType(request) == MemberType.NON_MEMBER;
    }
} 