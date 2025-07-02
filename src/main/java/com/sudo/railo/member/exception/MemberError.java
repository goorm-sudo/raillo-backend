package com.sudo.railo.member.exception;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberError implements ErrorCode {

	USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "M_001"),
	DUPLICATE_EMAIL("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT, "M_002"),
	INVALID_PASSWORD("비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED, "M_003"),
	DUPLICATE_GUEST_INFO("이미 동일한 비회원 정보가 존재합니다.", HttpStatus.CONFLICT, "M_004"),
	MEMBER_DELETE_FAIL("회원 삭제에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "M_005"),
	SAME_PHONE_NUMBER("현재 사용하는 휴대폰 번호와 동일합니다.", HttpStatus.CONFLICT, "M_006"),
	DUPLICATE_PHONE_NUMBER("이미 사용 중인 휴대폰 번호입니다.", HttpStatus.CONFLICT, "M_007"),
	SAME_EMAIL("현재 사용중인 이메일과 동일합니다.", HttpStatus.CONFLICT, "M_008");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
