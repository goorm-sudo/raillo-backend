package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(

	@NotBlank(message = "accessToken 값은 필수입니다.")
	String accessToken,

	@NotBlank(message = "refreshToken 값은 필수입니다.")
	String refreshToken
) {
}
