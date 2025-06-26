package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원번호 기반 로그인 요청 DTO")
public record MemberNoLoginRequest(

	@Schema(description = "회원의 고유 번호", example = "202506260001")
	@NotBlank(message = "회원번호는 필수입니다.")
	String memberNo,

	@Schema(description = "로그인 비밀번호", example = "password123!")
	@NotBlank(message = "비밀번호는 필수입니다.")
	String password
) {
}
