package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.domain.entity.MemberType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 회원 타입 판별 전문 도메인 서비스
 * - 회원/비회원 구분 로직
 * - 의존성 없는 순수 도메인 로직
 * - 결제 플로우 분기점 역할
 */
@Service
public class MemberTypeService {

    /**
     * 결제 요청에서 회원 타입을 판별
     * 
     * @param request 결제 실행 요청
     * @return 회원 타입 (MEMBER 또는 NON_MEMBER)
     * @throws IllegalArgumentException 회원 정보나 비회원 정보가 모두 없는 경우
     */
    public MemberType determineMemberType(PaymentExecuteRequest request) {
        // 1. 회원 ID 우선 확인
        if (request.getMemberId() != null && request.getMemberId() > 0) {
            return MemberType.MEMBER;
        }
        
        // 2. 비회원 정보 완전성 확인
        if (hasNonMemberInfo(request)) {
            return MemberType.NON_MEMBER;
        }
        
        // 3. 둘 다 없으면 예외 (Fail-Fast)
        throw new IllegalArgumentException("회원 ID 또는 비회원 정보가 필요합니다");
    }

    /**
     * 회원 여부 확인
     * 
     * @param request 결제 실행 요청
     * @return 회원이면 true
     */
    public boolean isMember(PaymentExecuteRequest request) {
        return determineMemberType(request) == MemberType.MEMBER;
    }

    /**
     * 비회원 여부 확인
     * 
     * @param request 결제 실행 요청
     * @return 비회원이면 true
     */
    public boolean isNonMember(PaymentExecuteRequest request) {
        return determineMemberType(request) == MemberType.NON_MEMBER;
    }

    /**
     * 비회원 정보 완전성 검증
     * 
     * @param request 결제 실행 요청
     * @return 비회원 정보가 완전하면 true
     */
    private boolean hasNonMemberInfo(PaymentExecuteRequest request) {
        return StringUtils.hasText(request.getNonMemberName())
            && StringUtils.hasText(request.getNonMemberPhone())
            && StringUtils.hasText(request.getNonMemberPassword());
    }
} 