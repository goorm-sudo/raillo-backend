package com.sudo.railo.global.security;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenError implements ErrorCode {

	INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, "T_001"),
	INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED, "T_002"),
	LOGOUT_ERROR("로그아웃: 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED, "T_003"),
	ALREADY_LOGOUT("이미 로그아웃된 토큰입니다.", HttpStatus.FORBIDDEN, "T_004");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
