package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 저장된 결제수단 엔티티
 */
@Entity
@Table(name = "saved_payment_methods")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SavedPaymentMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "payment_method_type", nullable = false, length = 50)
    private String paymentMethodType;

    @Column(name = "alias", length = 100)
    private String alias;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 신용카드 관련 필드 - 암호화 적용
    @Column(name = "card_number_encrypted", length = 500)
    private String cardNumberEncrypted;
    
    @Column(name = "card_number_hash", length = 100)
    private String cardNumberHash;  // 검색용 해시
    
    @Column(name = "card_last_four_digits", length = 4)
    private String cardLastFourDigits;  // 마스킹 표시용

    @Column(name = "card_holder_name_encrypted", length = 500)
    private String cardHolderNameEncrypted;

    @Column(name = "card_expiry_month_encrypted", length = 100)
    private String cardExpiryMonthEncrypted;

    @Column(name = "card_expiry_year_encrypted", length = 100)
    private String cardExpiryYearEncrypted;

    // 계좌 관련 필드 - 암호화 적용
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "account_number_encrypted", length = 500)
    private String accountNumberEncrypted;
    
    @Column(name = "account_number_hash", length = 100)
    private String accountNumberHash;  // 검색용 해시
    
    @Column(name = "account_last_four_digits", length = 4)
    private String accountLastFourDigits;  // 마스킹 표시용

    @Column(name = "account_holder_name_encrypted", length = 500)
    private String accountHolderNameEncrypted;
    
    @Column(name = "account_password_encrypted", length = 500)
    private String accountPasswordEncrypted;
    
    // 보안 관련 메타데이터
    @Column(name = "encryption_version", length = 10)
    private String encryptionVersion;  // 암호화 버전 (키 로테이션 지원)
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    /**
     * 마스킹된 카드번호 반환
     */
    @Transient
    public String getMaskedCardNumber() {
        if (cardLastFourDigits == null || cardLastFourDigits.isEmpty()) {
            return "****";
        }
        return "**** **** **** " + cardLastFourDigits;
    }
    
    /**
     * 마스킹된 계좌번호 반환
     */
    @Transient
    public String getMaskedAccountNumber() {
        if (accountLastFourDigits == null || accountLastFourDigits.isEmpty()) {
            return "****";
        }
        return "****" + accountLastFourDigits;
    }
    
    /**
     * 결제수단이 카드인지 확인
     */
    @Transient
    public boolean isCard() {
        return "CARD".equalsIgnoreCase(paymentMethodType) || 
               "CREDIT_CARD".equalsIgnoreCase(paymentMethodType) ||
               "DEBIT_CARD".equalsIgnoreCase(paymentMethodType);
    }
    
    /**
     * 결제수단이 계좌인지 확인
     */
    @Transient
    public boolean isAccount() {
        return "ACCOUNT".equalsIgnoreCase(paymentMethodType) || 
               "BANK_ACCOUNT".equalsIgnoreCase(paymentMethodType);
    }
    
    /**
     * 사용 시간 업데이트
     */
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * 기본 결제수단으로 설정
     */
    public void setAsDefault() {
        this.isDefault = true;
    }
    
    /**
     * 기본 결제수단 해제
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }
    
    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * 카드 정보 암호화 설정
     */
    public void setEncryptedCardInfo(String encryptedNumber, String numberHash, String lastFourDigits,
                                      String encryptedHolderName, String encryptedExpiryMonth, 
                                      String encryptedExpiryYear) {
        this.cardNumberEncrypted = encryptedNumber;
        this.cardNumberHash = numberHash;
        this.cardLastFourDigits = lastFourDigits;
        this.cardHolderNameEncrypted = encryptedHolderName;
        this.cardExpiryMonthEncrypted = encryptedExpiryMonth;
        this.cardExpiryYearEncrypted = encryptedExpiryYear;
    }
    
    /**
     * 계좌 정보 암호화 설정
     */
    public void setEncryptedAccountInfo(String encryptedNumber, String numberHash, String lastFourDigits,
                                         String encryptedHolderName, String encryptedPassword) {
        this.accountNumberEncrypted = encryptedNumber;
        this.accountNumberHash = numberHash;
        this.accountLastFourDigits = lastFourDigits;
        this.accountHolderNameEncrypted = encryptedHolderName;
        this.accountPasswordEncrypted = encryptedPassword;
    }
    
    /**
     * 암호화 버전 업데이트
     */
    public void updateEncryptionVersion(String version) {
        this.encryptionVersion = version;
    }
    
    /**
     * 엔티티 저장 전 시간 설정
     */
    @PrePersist
    public void prePersist() {
        if (getCreatedAt() == null) {
            // BaseEntity의 @CreatedDate가 작동하지 않을 경우를 대비
            try {
                java.lang.reflect.Field createdAtField = BaseEntity.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(this, LocalDateTime.now());
                
                java.lang.reflect.Field updatedAtField = BaseEntity.class.getDeclaredField("updatedAt");
                updatedAtField.setAccessible(true);
                updatedAtField.set(this, LocalDateTime.now());
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    /**
     * 엔티티 업데이트 전 시간 설정
     */
    @PreUpdate
    public void preUpdate() {
        try {
            java.lang.reflect.Field updatedAtField = BaseEntity.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(this, LocalDateTime.now());
        } catch (Exception e) {
            // ignore
        }
    }
} 