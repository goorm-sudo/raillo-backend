package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증코드 검증 응답 DTO")
public record VerifyCodeResponse(

	@Schema(description = "이메일 인증 성공 여부", example = "true")
	boolean isVerified
) {
}
