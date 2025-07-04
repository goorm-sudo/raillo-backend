package com.sudo.railo.member.exception;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthError implements ErrorCode {

	SEND_EMAIL_FAIL("인증 이메일 전송이 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "E_001");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
