package com.sudo.railo.member.success;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccess implements SuccessCode {

	// 비회원
	GUEST_REGISTER_SUCCESS(HttpStatus.CREATED, "비회원 정보 등록이 성공적으로 완료되었습니다."),

	// 회원
	MEMBER_DELETE_SUCCESS(HttpStatus.OK, "회원 탈퇴가 성공적으로 완료되었습니다."),
	MEMBER_INFO_SUCCESS(HttpStatus.OK, "회원 정보 조회에 성공했습니다."),
	MEMBER_EMAIL_UPDATE_SUCCESS(HttpStatus.OK, "이메일 변경에 성공했습니다."),
	MEMBER_PHONENUMBER_UPDATE_SUCCESS(HttpStatus.OK, "휴대폰 번호 변경에 성공했습니다.");

	private final HttpStatus status;
	private final String message;
}
