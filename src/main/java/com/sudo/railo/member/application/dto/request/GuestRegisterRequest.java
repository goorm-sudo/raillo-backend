package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "게스트 등록 요청 DTO")
public record GuestRegisterRequest(

	@Schema(description = "사용자의 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@Schema(description = "사용자의 전화번호 (- 없이 입력)", example = "01012345678")
	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 -를 제외한 11자리 숫자만 가능합니다.")
	String phoneNumber,

	@Schema(description = "5자리의 숫자로 이루어진 비밀번호", example = "12345")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{5}$", message = "비밀번호는 5자리 숫자만 가능합니다.")
	String password
) {
}
