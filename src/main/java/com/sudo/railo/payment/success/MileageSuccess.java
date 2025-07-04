package com.sudo.railo.payment.success;

import com.sudo.railo.global.success.SuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MileageSuccess implements SuccessCode {
    
    MILEAGE_BALANCE_SUCCESS(HttpStatus.OK, "마일리지 잔액 조회가 성공적으로 완료되었습니다."),
    MILEAGE_AVAILABLE_SUCCESS(HttpStatus.OK, "사용 가능한 마일리지 조회가 성공적으로 완료되었습니다."),
    MILEAGE_STATISTICS_SUCCESS(HttpStatus.OK, "마일리지 통계 조회가 성공적으로 완료되었습니다."),
    MILEAGE_EARNING_SUCCESS(HttpStatus.OK, "마일리지 적립이 성공적으로 완료되었습니다."),
    MILEAGE_USAGE_SUCCESS(HttpStatus.OK, "마일리지 사용이 성공적으로 완료되었습니다.");
    
    private final HttpStatus status;
    private final String message;
} 