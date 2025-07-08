package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "임시 토큰 발급 DTO")
public record TemporaryTokenResponse(

	@Schema(description = "임시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	String temporaryToken
) {
}
