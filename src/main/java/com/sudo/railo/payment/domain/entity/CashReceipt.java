package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.infrastructure.persistence.converter.CashReceiptTypeConverter;
import jakarta.persistence.*;
import lombok.*;

/**
 * 현금영수증 Value Object
 * 
 * Payment 엔티티에서 현금영수증 관련 필드들을 분리하여 응집도를 높임
 */
@Embeddable
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CashReceipt {
    
    @Builder.Default
    @Column(name = "receipt_requested")
    private Boolean requested = false;
    
    @Convert(converter = CashReceiptTypeConverter.class)
    @Column(name = "receipt_type")
    private CashReceiptType type;
    
    @Column(name = "receipt_phone_number")
    private String phoneNumber;
    
    @Column(name = "receipt_business_number")
    private String businessNumber;
    
    @Column(name = "receipt_url")
    private String receiptUrl;
    
    /**
     * 현금영수증 타입
     */
    public enum CashReceiptType {
        PERSONAL("personal", "개인 소득공제"),
        BUSINESS("business", "사업자 증빙");
        
        private final String code;
        private final String description;
        
        CashReceiptType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static CashReceiptType fromCode(String code) {
            for (CashReceiptType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid cash receipt type code: " + code);
        }
    }
    
    /**
     * 현금영수증 요청 여부 확인
     */
    public boolean isRequested() {
        return Boolean.TRUE.equals(this.requested);
    }
    
    /**
     * 현금영수증 정보 검증
     */
    public void validate() {
        if (!isRequested()) {
            return;
        }
        
        if (type == null) {
            throw new PaymentValidationException("현금영수증 타입이 필요합니다");
        }
        
        if (type == CashReceiptType.PERSONAL) {
            validatePersonalReceipt();
        } else if (type == CashReceiptType.BUSINESS) {
            validateBusinessReceipt();
        }
    }
    
    /**
     * 개인 소득공제용 현금영수증 검증
     */
    private void validatePersonalReceipt() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new PaymentValidationException("개인 소득공제용 현금영수증은 휴대폰 번호가 필요합니다");
        }
        
        // 휴대폰 번호 형식 검증 (간단한 검증)
        String cleanedPhone = phoneNumber.replaceAll("[^0-9]", "");
        if (cleanedPhone.length() != 11 || !cleanedPhone.startsWith("010")) {
            throw new PaymentValidationException("올바른 휴대폰 번호 형식이 아닙니다");
        }
    }
    
    /**
     * 사업자 증빙용 현금영수증 검증
     */
    private void validateBusinessReceipt() {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            throw new PaymentValidationException("사업자 증빙용 현금영수증은 사업자등록번호가 필요합니다");
        }
        
        // 사업자등록번호 형식 검증 (간단한 검증)
        String cleanedBizNo = businessNumber.replaceAll("[^0-9]", "");
        if (cleanedBizNo.length() != 10) {
            throw new PaymentValidationException("올바른 사업자등록번호 형식이 아닙니다 (10자리)");
        }
    }
    
    
    /**
     * 개인 소득공제용 현금영수증 생성 팩토리 메서드
     */
    public static CashReceipt createPersonalReceipt(String phoneNumber) {
        return CashReceipt.builder()
                .requested(true)
                .type(CashReceiptType.PERSONAL)
                .phoneNumber(phoneNumber)
                .build();
    }
    
    /**
     * 사업자 증빙용 현금영수증 생성 팩토리 메서드
     */
    public static CashReceipt createBusinessReceipt(String businessNumber) {
        return CashReceipt.builder()
                .requested(true)
                .type(CashReceiptType.BUSINESS)
                .businessNumber(businessNumber)
                .build();
    }
    
    /**
     * 현금영수증 미신청 생성 팩토리 메서드
     */
    public static CashReceipt notRequested() {
        return CashReceipt.builder()
                .requested(false)
                .build();
    }
    
    /**
     * 현금영수증 타입 조회 (하위 호환성)
     * getType()의 별칭
     */
    public CashReceiptType getReceiptType() {
        return type;
    }
}