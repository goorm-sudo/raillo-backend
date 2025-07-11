package com.sudo.railo.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원번호 찾기 검증 응답 DTO")
public record VerifyMemberNoResponse(

	@Schema(description = "찾아온 회원 번호", example = "202506260001")
	String memberNo
) {
}
