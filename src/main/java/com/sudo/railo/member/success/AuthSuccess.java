package com.sudo.railo.member.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthSuccess implements SuccessCode {

	SIGN_UP_SUCCESS(HttpStatus.CREATED, "회원가입이 성공적으로 완료되었습니다."),
	MEMBER_NO_LOGIN_SUCCESS(HttpStatus.OK, "회원번호 로그인이 성공적으로 완료되었습니다."),
	LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃이 성공적으로 완료되었습니다."),
	REISSUE_TOKEN_SUCCESS(HttpStatus.OK, "AccessToken 이 성공적으로 발급 되었습니다."),

	// 이메일 인증 관련
	SEND_CODE_SUCCESS(HttpStatus.OK, "이메일 인증 코드 전송을 성공했습니다."),
	VERIFY_CODE_SUCCESS(HttpStatus.OK, "이메일 인증 코드 검증에 성공했습니다.");

	private final HttpStatus status;
	private final String message;

}
