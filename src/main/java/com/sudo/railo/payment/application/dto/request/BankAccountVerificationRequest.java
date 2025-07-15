package com.sudo.railo.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 은행 계좌 검증 요청 DTO
 * 계좌 유효성 검증을 위한 최소 정보만 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountVerificationRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "계좌번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "계좌번호는 10-20자리 숫자여야 합니다.")
    private String accountNumber;

    @NotBlank(message = "계좌 비밀번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{4}$", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
    private String accountPassword;
}