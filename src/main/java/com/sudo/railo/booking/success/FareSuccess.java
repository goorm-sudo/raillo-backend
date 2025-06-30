package com.sudo.railo.booking.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FareSuccess implements SuccessCode {

	FARE_CALCULATE_SUCCESS(HttpStatus.OK, "정상적으로 계산되었습니다.");

	private final HttpStatus status;
	private final String message;
}
