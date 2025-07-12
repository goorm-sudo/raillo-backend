package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.BankAccountVerificationRequest;
import com.sudo.railo.payment.application.dto.response.BankAccountVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 은행 계좌 검증 서비스
 * Mock 환경에서는 항상 성공 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountVerificationService {

    // 은행 코드 매핑
    private static final Map<String, String> BANK_NAMES = new HashMap<>() {{
        put("004", "국민은행");
        put("088", "신한은행");
        put("020", "우리은행");
        put("081", "하나은행");
        put("011", "농협은행");
        put("032", "부산은행");
        put("031", "대구은행");
        put("034", "광주은행");
        put("037", "전북은행");
        put("039", "경남은행");
        put("035", "제주은행");
        put("090", "카카오뱅크");
        put("089", "케이뱅크");
        put("092", "토스뱅크");
        put("003", "IBK기업은행");
        put("국민은행", "국민은행");
        put("신한은행", "신한은행");
        put("우리은행", "우리은행");
        put("하나은행", "하나은행");
        put("농협은행", "농협은행");
        put("부산은행", "부산은행");
        put("대구은행", "대구은행");
        put("광주은행", "광주은행");
        put("전북은행", "전북은행");
        put("경남은행", "경남은행");
        put("제주은행", "제주은행");
        put("카카오뱅크", "카카오뱅크");
        put("케이뱅크", "케이뱅크");
        put("토스뱅크", "토스뱅크");
        put("IBK기업은행", "IBK기업은행");
    }};

    /**
     * 계좌 유효성 검증
     * Mock 환경에서는 항상 성공
     */
    public BankAccountVerificationResponse verifyAccount(BankAccountVerificationRequest request) {
        log.info("계좌 검증 시작 - 은행: {}, 계좌번호: {}", 
                request.getBankCode(), 
                maskAccountNumber(request.getAccountNumber()));

        // Mock 환경에서는 항상 성공
        String bankName = BANK_NAMES.getOrDefault(request.getBankCode(), request.getBankCode());
        String maskedAccountNumber = maskAccountNumber(request.getAccountNumber());
        
        log.info("계좌 검증 성공 (Mock) - 은행: {}, 계좌번호: {}", bankName, maskedAccountNumber);
        
        return BankAccountVerificationResponse.success(
                "홍길동", // Mock 예금주명
                maskedAccountNumber,
                bankName
        );
    }

    /**
     * 계좌번호 마스킹
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return "****";
        }
        int length = accountNumber.length();
        return accountNumber.substring(0, 4) + "****" + accountNumber.substring(length - 4);
    }
}