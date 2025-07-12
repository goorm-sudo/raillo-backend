package com.sudo.railo.booking.exception;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingError implements ErrorCode {

	SEAT_NOT_FOUND("좌석을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "B_001"),
	SEAT_ALREADY_RESERVED("이미 예약된 좌석입니다.", HttpStatus.CONFLICT, "B_002"),
	SEAT_RESERVATION_FAILED("좌석 예약에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_003"),
	SEAT_CANCELLATION_FAILED("좌석 취소에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_004"),
	SEAT_ALREADY_CANCELLED("이미 취소된 좌석입니다.", HttpStatus.CONFLICT, "B_005"),
	SEAT_NOT_AVAILABLE("사용가능한 좌석이 아닙니다.", HttpStatus.BAD_REQUEST, "B_006"),
	SEAT_NOT_RESERVED("예약된 좌석이 아닙니다.", HttpStatus.BAD_REQUEST, "B_007"),
	EXPIRED_SEAT_CANCELLATION_FAILED("만료된 좌석 취소에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_008"),
	RESERVATION_CREATE_FAILED("예약에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_009"),
	RESERVATION_DELETE_FAILED("예약 취소에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_010"),
	TRAIN_NOT_OPERATIONAL("운행중인 스케줄이 아닙니다.", HttpStatus.BAD_REQUEST, "B_011"),
	RESERVATION_CREATE_SEATS_INVALID("좌석 수는 총 승객 수와 같아야 합니다.", HttpStatus.BAD_REQUEST, "B_012"),
	QR_CREATE_FAILED("QR 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_013"),
	TICKET_CREATE_FAILED("티켓 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "B_014");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
