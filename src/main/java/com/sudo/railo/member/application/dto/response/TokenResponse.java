package com.sudo.railo.member.application.dto.response;

public record TokenResponse(

	String grantType,          // 토큰 타입
	String accessToken,        // 액세스 토큰
	Long accessTokenExpiresIn, // 만료 시간
	String refreshToken        // 리프레시 토큰
) {
}
