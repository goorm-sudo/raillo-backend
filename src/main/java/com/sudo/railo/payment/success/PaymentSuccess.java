package com.sudo.railo.payment.success;

import com.sudo.railo.global.success.SuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentSuccess implements SuccessCode {
    
    // 마일리지 관련 성공 메시지
    MILEAGE_INQUIRY_SUCCESS(HttpStatus.OK, "마일리지 조회가 성공적으로 완료되었습니다."),
    MILEAGE_SCHEDULE_INQUIRY_SUCCESS(HttpStatus.OK, "마일리지 적립 스케줄 조회가 성공적으로 완료되었습니다."),
    MILEAGE_TRANSACTION_INQUIRY_SUCCESS(HttpStatus.OK, "마일리지 거래 내역 조회가 성공적으로 완료되었습니다."),
    
    // 관리자 기능 성공 메시지
    TRAIN_ARRIVAL_RECORDED_SUCCESS(HttpStatus.OK, "열차 도착 기록이 성공적으로 완료되었습니다."),
    TRAIN_DELAY_RECORDED_SUCCESS(HttpStatus.OK, "열차 지연 기록이 성공적으로 완료되었습니다."),
    STATISTICS_INQUIRY_SUCCESS(HttpStatus.OK, "통계 조회가 성공적으로 완료되었습니다."),
    BATCH_PROCESS_SUCCESS(HttpStatus.OK, "배치 처리가 성공적으로 완료되었습니다."),
    DATA_CLEANUP_SUCCESS(HttpStatus.OK, "데이터 정리가 성공적으로 완료되었습니다."),
    EVENT_RECOVERY_SUCCESS(HttpStatus.OK, "이벤트 복구가 성공적으로 완료되었습니다."),
    
    // 일반적인 성공 메시지
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
    
    private final HttpStatus status;
    private final String message;
} 