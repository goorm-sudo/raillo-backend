package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.domain.entity.MemberType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MemberTypeServiceTest {
    
    private final MemberTypeService memberTypeService = new MemberTypeService();
    
    @Test
    @DisplayName("회원 ID가 있으면 MEMBER로 판별")
    void determineMemberType_WithMemberId_ReturnsMember() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .memberId(123L)
            .build();
        
        // when
        MemberType result = memberTypeService.determineMemberType(request);
        
        // then
        assertThat(result).isEqualTo(MemberType.MEMBER);
    }
    
    @Test
    @DisplayName("비회원 정보가 완전하면 NON_MEMBER로 판별")
    void determineMemberType_WithNonMemberInfo_ReturnsNonMember() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("010-1234-5678")
            .nonMemberPassword("password123")
            .build();
        
        // when
        MemberType result = memberTypeService.determineMemberType(request);
        
        // then
        assertThat(result).isEqualTo(MemberType.NON_MEMBER);
    }
    
    @Test
    @DisplayName("회원 ID와 비회원 정보가 모두 없으면 예외 발생")
    void determineMemberType_WithoutAnyInfo_ThrowsException() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .build();
        
        // when & then
        assertThatThrownBy(() -> memberTypeService.determineMemberType(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 ID 또는 비회원 정보가 필요합니다");
    }
    
    @Test
    @DisplayName("비회원 정보가 불완전하면 예외 발생")
    void determineMemberType_WithIncompleteNonMemberInfo_ThrowsException() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("010-1234-5678")
            // 비밀번호 누락
            .build();
        
        // when & then
        assertThatThrownBy(() -> memberTypeService.determineMemberType(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 ID 또는 비회원 정보가 필요합니다");
    }
} 