package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "휴대폰 번호 변경 요청 DTO")
public record UpdatePhoneNumberRequest(

	@Schema(description = "전화번호, '-' 없이 11자리 숫자로 입력", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 -를 제외한 11자리 숫자만 가능합니다.")
	String newPhoneNumber
) {
}
