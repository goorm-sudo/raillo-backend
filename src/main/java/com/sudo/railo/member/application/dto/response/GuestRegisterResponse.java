package com.sudo.railo.member.application.dto.response;

import com.sudo.railo.member.domain.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게스트 등록 응답 DTO")
public record GuestRegisterResponse(

	@Schema(description = "게스트의 이름", example = "홍길동")
	String name,

	@Schema(description = "게스트의 권한(Role)", example = "GUEST")
	Role role
) {
}
