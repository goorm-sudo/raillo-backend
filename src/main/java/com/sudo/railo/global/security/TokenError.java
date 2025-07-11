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
	ALREADY_LOGOUT("이미 로그아웃된 토큰입니다.", HttpStatus.FORBIDDEN, "T_004"),
	INVALID_PASSWORD("비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, "T_005"),
	AUTHORITY_NOT_FOUND("권한 정보가 없는 토큰입니다.", HttpStatus.UNAUTHORIZED, "T_006"),
	NOT_EQUALS_REFRESH_TOKEN("리프레시 토큰이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, "T_007"),
	INVALID_TEMPORARY_TOKEN_USAGE("임시토큰으로 해당 요청에 접근할 수 없습니다.", HttpStatus.UNAUTHORIZED, "T_008");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
