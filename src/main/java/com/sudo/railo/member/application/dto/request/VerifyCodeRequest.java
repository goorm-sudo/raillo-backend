package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "인증코드 검증 요청 DTO")
public record VerifyCodeRequest(

	@Schema(description = "인증코드가 전송된 이메일 주소", example = "hong@email.com")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email,

	@Schema(description = "인증코드 6자리", example = "335262")
	@NotBlank(message = "인증 코드는 반드시 입력해야 합니다.")
	@Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.")
	String authCode
) {
}
