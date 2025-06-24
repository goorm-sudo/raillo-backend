package com.sudo.railo.member.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccess implements SuccessCode {

	GUEST_REGISTER_SUCCESS(HttpStatus.CREATED, "비회원 정보 등록이 성공적으로 완료되었습니다.");

	private final HttpStatus status;
	private final String message;
}
