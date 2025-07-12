package com.sudo.railo.payment.application.util;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 결제 도메인 공통 유틸리티 클래스
 * 중복 코드 제거 및 공통 로직 관리
 */
@Slf4j
@Component
public class PaymentUtils {
    
    /**
     * 전화번호 마스킹 처리
     * @param phone 원본 전화번호
     * @return 마스킹된 전화번호
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * 결제 소유권 검증
     * @param payment 결제 정보
     * @param memberId 회원 ID
     * @throws PaymentValidationException 본인 결제가 아닌 경우
     */
    public static void validatePaymentOwnership(Payment payment, Long memberId) {
        if (payment.getMemberId() == null) {
            throw new PaymentValidationException("회원 결제 정보가 아닙니다");
        }
        if (!payment.getMemberId().equals(memberId)) {
            throw new PaymentValidationException("본인의 결제 내역만 조회할 수 있습니다");
        }
    }
    
    /**
     * 재시도 로직을 포함한 작업 실행
     * @param operation 실행할 작업
     * @param maxRetries 최대 재시도 횟수
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("작업 실행 실패, 재시도 중... (시도: {}/{})", attempt + 1, maxRetries + 1, e);
                    try {
                        Thread.sleep(100L * (attempt + 1)); // 점진적 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                } else {
                    log.error("모든 재시도 실패", e);
                }
            }
        }
        
        throw new RuntimeException("최대 재시도 횟수 초과", lastException);
    }
    
    /**
     * 비회원 정보 검증
     * @param name 이름
     * @param phone 전화번호
     * @throws PaymentValidationException 검증 실패시
     */
    public static void validateNonMemberInfo(String name, String phone) {
        if (name == null || name.trim().isEmpty()) {
            throw new PaymentValidationException("비회원 이름은 필수입니다");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new PaymentValidationException("비회원 전화번호는 필수입니다");
        }
        if (!isValidPhoneNumber(phone)) {
            throw new PaymentValidationException("올바른 전화번호 형식이 아닙니다");
        }
    }
    
    /**
     * 전화번호 형식 검증
     * @param phone 전화번호
     * @return 유효성 여부
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return cleanPhone.length() >= 10 && cleanPhone.length() <= 11;
    }
    
    /**
     * 전화번호 정규화 (숫자만 남기고 010xxxxxxxx 형태로)
     * @param phone 원본 전화번호
     * @return 정규화된 전화번호
     */
    public static String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
    
    /**
     * 안전한 문자열 비교 (null 안전)
     * @param str1 문자열1
     * @param str2 문자열2
     * @return 같으면 true
     */
    public static boolean safeEquals(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
}
