package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증코드 전송 응답 DTO")
public record SendCodeResponse(

	@Schema(description = "인증코드가 전송된 이메일", example = "hong@email.com")
	String email
) {
}
