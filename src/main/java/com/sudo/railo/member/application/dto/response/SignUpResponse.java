package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public record SignUpResponse(

	@Schema(description = "생성된 회원 번호", example = "202506260001")
	String memberNo
) {
}
