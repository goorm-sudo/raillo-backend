package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 변경 요청 DTO")
public record UpdatePasswordRequest(

	@Schema(description = "사용자의 비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "비밀번호는 필수입니다.")
	String newPassword
) {
}
