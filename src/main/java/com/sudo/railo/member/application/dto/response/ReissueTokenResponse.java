package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "accessToken 재발급 응답 DTO")
public record ReissueTokenResponse(

	@Schema(description = "토큰의 타입 (예: Bearer)", example = "Bearer")
	String grantType,

	@Schema(description = "발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	String accessToken,

	@Schema(description = "액세스 토큰의 만료 시간", example = "1750508812329")
	Long accessTokenExpiresIn
) {
}
