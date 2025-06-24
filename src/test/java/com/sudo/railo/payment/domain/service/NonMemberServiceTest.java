package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.exception.PaymentValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NonMemberServiceTest {
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    private NonMemberService nonMemberService;
    
    @BeforeEach
    void setUp() {
        nonMemberService = new NonMemberService(passwordEncoder);
    }
    
    @Test
    @DisplayName("비회원 정보 저장 성공")
    void saveNonMemberInfo_Success() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("010-1234-5678")
            .nonMemberPassword("password123")
            .build();
        
        Payment payment = Payment.builder().build();
        
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        
        // when
        nonMemberService.saveNonMemberInfo(payment, request);
        
        // then
        assertThat(payment.getNonMemberName()).isEqualTo("홍길동");
        assertThat(payment.getNonMemberPhone()).isEqualTo("01012345678");
        assertThat(payment.getNonMemberPassword()).isEqualTo("encodedPassword");
        
        verify(passwordEncoder).encode("password123");
    }
    
    @Test
    @DisplayName("비회원 이름이 없으면 예외 발생")
    void saveNonMemberInfo_WithoutName_ThrowsException() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .nonMemberPhone("010-1234-5678")
            .nonMemberPassword("password123")
            .build();
        
        Payment payment = Payment.builder().build();
        
        // when & then
        assertThatThrownBy(() -> nonMemberService.saveNonMemberInfo(payment, request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("비회원 정보가 완전하지 않습니다");
    }
    
    @Test
    @DisplayName("잘못된 전화번호 형식이면 예외 발생")
    void saveNonMemberInfo_WithInvalidPhone_ThrowsException() {
        // given
        PaymentExecuteRequest request = PaymentExecuteRequest.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("123-456-7890")  // 잘못된 형식
            .nonMemberPassword("password123")
            .build();
        
        Payment payment = Payment.builder().build();
        
        // when & then
        assertThatThrownBy(() -> nonMemberService.saveNonMemberInfo(payment, request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("올바른 전화번호 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("비회원 정보 검증 성공")
    void validateNonMemberInfo_Success() {
        // given
        Payment payment = Payment.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("01012345678")
            .nonMemberPassword("encodedPassword")
            .build();
        
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        
        // when
        boolean result = nonMemberService.validateNonMemberInfo(
            "홍길동", "010-1234-5678", "password123", payment);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("이름이 다르면 검증 실패")
    void validateNonMemberInfo_DifferentName_ReturnsFalse() {
        // given
        Payment payment = Payment.builder()
            .nonMemberName("홍길동")
            .nonMemberPhone("01012345678")
            .nonMemberPassword("encodedPassword")
            .build();
        
        // when
        boolean result = nonMemberService.validateNonMemberInfo(
            "김철수", "010-1234-5678", "password123", payment);
        
        // then
        assertThat(result).isFalse();
    }
} 