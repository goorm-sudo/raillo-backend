package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 비회원 정보 처리 도메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonMemberService {
    
    private final PasswordEncoder passwordEncoder;
    
    // 전화번호 정규식 (010-1234-5678 또는 01012345678)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789](-)?\\d{3,4}(-)?\\d{4}$");
    
    /**
     * 비회원 정보를 Payment 엔티티에 저장
     */
    public void saveNonMemberInfo(Payment payment, PaymentExecuteRequest request) {
        if (!StringUtils.hasText(request.getNonMemberName()) ||
            !StringUtils.hasText(request.getNonMemberPhone()) ||
            !StringUtils.hasText(request.getNonMemberPassword())) {
            throw new PaymentValidationException("비회원 정보가 완전하지 않습니다");
        }
        
        // 전화번호 검증
        validatePhoneNumber(request.getNonMemberPhone());
        
        // 비밀번호 암호화하여 저장
        String encodedPassword = passwordEncoder.encode(request.getNonMemberPassword());
        
        payment.setNonMemberName(request.getNonMemberName().trim());
        payment.setNonMemberPhone(normalizePhoneNumber(request.getNonMemberPhone()));
        payment.setNonMemberPassword(encodedPassword);
        
        log.info("비회원 정보 저장 완료 - 이름: {}, 전화번호: {}", 
                request.getNonMemberName(), maskPhoneNumber(request.getNonMemberPhone()));
    }
    
    /**
     * 비회원 정보 검증 (결제 내역 조회용)
     */
    public boolean validateNonMemberInfo(String name, String phone, String password, Payment payment) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return false;
        }
        
        // 이름 검증
        if (!name.trim().equals(payment.getNonMemberName())) {
            return false;
        }
        
        // 전화번호 검증 (정규화하여 비교)
        String normalizedInputPhone = normalizePhoneNumber(phone);
        if (!normalizedInputPhone.equals(payment.getNonMemberPhone())) {
            return false;
        }
        
        // 비밀번호 검증
        return passwordEncoder.matches(password, payment.getNonMemberPassword());
    }
    
    /**
     * 전화번호 유효성 검증
     */
    private void validatePhoneNumber(String phone) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (!PHONE_PATTERN.matcher(phone).matches() && !cleanPhone.matches("^01[016789]\\d{7,8}$")) {
            throw new PaymentValidationException("올바른 전화번호 형식이 아닙니다");
        }
    }
    
    /**
     * 전화번호 정규화 (숫자만 남기고 010xxxxxxxx 형태로)
     */
    private String normalizePhoneNumber(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
    
    /**
     * 전화번호 마스킹 (로그용)
     */
    private String maskPhoneNumber(String phone) {
        String normalized = normalizePhoneNumber(phone);
        if (normalized.length() >= 7) {
            return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
        }
        return "****";
    }
} 