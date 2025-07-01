package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 저장된 결제수단 엔티티
 */
@Entity
@Table(name = "saved_payment_methods")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
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

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 신용카드 관련 필드
    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "card_expiry_month", length = 2)
    private String cardExpiryMonth;

    @Column(name = "card_expiry_year", length = 4)
    private String cardExpiryYear;

    // 계좌 관련 필드
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;
} 