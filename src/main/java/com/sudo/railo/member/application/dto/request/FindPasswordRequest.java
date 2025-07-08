package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 찾기 요청 DTO")
public record FindPasswordRequest(

	@Schema(description = "사용자의 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@Schema(description = "회원의 고유 번호", example = "202506260001")
	@NotBlank(message = "회원번호는 필수입니다.")
	String memberNo
) {
}
