package com.sudo.railo.payment.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 은행 계좌 검증 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountVerificationResponse {

    private boolean verified;
    private String accountHolderName;
    private String maskedAccountNumber;
    private String bankName;
    private String message;

    /**
     * 검증 성공 응답 생성
     */
    public static BankAccountVerificationResponse success(String accountHolderName, 
                                                        String maskedAccountNumber, 
                                                        String bankName) {
        return BankAccountVerificationResponse.builder()
                .verified(true)
                .accountHolderName(accountHolderName)
                .maskedAccountNumber(maskedAccountNumber)
                .bankName(bankName)
                .message("계좌 인증이 완료되었습니다.")
                .build();
    }

    /**
     * 검증 실패 응답 생성
     */
    public static BankAccountVerificationResponse failure(String message) {
        return BankAccountVerificationResponse.builder()
                .verified(false)
                .message(message)
                .build();
    }
}