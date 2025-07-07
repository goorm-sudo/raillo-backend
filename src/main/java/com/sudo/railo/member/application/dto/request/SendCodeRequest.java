package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "인증코드 전송 요청 DTO")
public record SendCodeRequest(

	@Schema(description = "사용자의 이메일 주소", example = "hong@example.com")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email
) {
}
