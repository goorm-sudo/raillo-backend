package com.sudo.railo.booking.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CartReservationSuccess implements SuccessCode {

	CART_RESERVATION_CREATE_SUCCESS(HttpStatus.CREATED, "장바구니에 예약이 등록되었습니다."),
	CART_RESERVATION_LIST_SUCCESS(HttpStatus.OK, "장바구니를 성공적으로 조회했습니다.");

	private final HttpStatus status;
	private final String message;
}
