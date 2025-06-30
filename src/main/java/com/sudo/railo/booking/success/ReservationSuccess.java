package com.sudo.railo.booking.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationSuccess implements SuccessCode {

	RESERVATION_CREATE_SUCCESS(HttpStatus.CREATED, "예약이 성공적으로 생성되었습니다."),
	RESERVATION_DELETE_SUCCESS(HttpStatus.NO_CONTENT, "예약이 성공적으로 삭제되었습니다.");

	private final HttpStatus status;
	private final String message;
}
