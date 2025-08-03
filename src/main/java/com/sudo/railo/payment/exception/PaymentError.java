package com.sudo.railo.payment.exception;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PaymentError implements ErrorCode {

	// 예약 관련 에러
	RESERVATION_NOT_FOUND("예약을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "P_001"),
	RESERVATION_ACCESS_DENIED("예약에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN, "P_002"),
	RESERVATION_NOT_PAYABLE("결제할 수 없는 예약 상태입니다.", HttpStatus.BAD_REQUEST, "P_003"),

	// 결제 관련 에러
	PAYMENT_NOT_FOUND("결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "P_004"),
	PAYMENT_ACCESS_DENIED("결제에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN, "P_005"),
	PAYMENT_ALREADY_COMPLETED("이미 결제가 완료된 예약입니다.", HttpStatus.BAD_REQUEST, "P_006"),
	PAYMENT_NOT_CANCELLABLE("취소할 수 없는 결제 상태입니다.", HttpStatus.BAD_REQUEST, "P_007"),
	PAYMENT_NOT_APPROVABLE("승인할 수 없는 결제 상태입니다.", HttpStatus.BAD_REQUEST, "P_008"),
	PAYMENT_PROCESS_FAILED("결제 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "P_009"),

	// 금액 관련 에러
	INVALID_PAYMENT_AMOUNT("유효하지 않은 결제 금액입니다.", HttpStatus.BAD_REQUEST, "P_010"),
	PAYMENT_AMOUNT_MISMATCH("결제 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST, "P_011"),

	// 결제 수단 관련 에러
	INVALID_PAYMENT_METHOD("지원하지 않는 결제 수단입니다.", HttpStatus.BAD_REQUEST, "P_012"),

	// 시스템 에러
	PAYMENT_SYSTEM_ERROR("결제 시스템 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "P_999");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
