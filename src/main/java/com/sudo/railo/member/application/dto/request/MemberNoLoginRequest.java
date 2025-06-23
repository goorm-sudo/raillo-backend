package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MemberNoLoginRequest(

	@NotBlank(message = "회원번호는 필수입니다.")
	String memberNo,

	@NotBlank(message = "비밀번호는 필수입니다.")
	String password
) {
}
