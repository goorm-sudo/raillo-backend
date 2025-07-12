package com.sudo.railo.payment.success;

import org.springframework.http.HttpStatus;
import com.sudo.railo.global.success.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PgPaymentSuccess implements SuccessCode {

    PG_PAYMENT_REQUEST_SUCCESS(HttpStatus.OK, "PG 결제 요청이 성공적으로 처리되었습니다."),
    PG_PAYMENT_APPROVE_SUCCESS(HttpStatus.OK, "PG 결제 승인이 성공적으로 완료되었습니다."),
    PG_PAYMENT_CANCEL_SUCCESS(HttpStatus.OK, "PG 결제 취소가 성공적으로 처리되었습니다."),
    PG_PAYMENT_STATUS_SUCCESS(HttpStatus.OK, "PG 결제 상태 조회가 성공적으로 완료되었습니다.");

    private final HttpStatus status;
    private final String message;
} 