package com.sudo.railo.payment.success;

import com.sudo.railo.global.success.SuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentCalculationSuccess implements SuccessCode {
    
    PAYMENT_CALCULATION_SUCCESS(HttpStatus.OK, "결제 계산이 완료되었습니다."),
    PAYMENT_CALCULATION_RETRIEVAL_SUCCESS(HttpStatus.OK, "결제 계산 정보를 조회했습니다.");
    
    private final HttpStatus status;
    private final String message;
}