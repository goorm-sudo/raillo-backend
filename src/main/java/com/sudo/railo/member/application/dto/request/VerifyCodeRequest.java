package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(

	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email,

	@NotBlank(message = "인증 코드는 반드시 입력해야 합니다.")
	@Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.")
	String authCode
) {
}
