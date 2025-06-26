package com.sudo.railo.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원가입 요청 DTO")
public record SignUpRequest(

	@Schema(description = "사용자의 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@Schema(description = "전화번호, '-' 없이 11자리 숫자로 입력", example = "01012345678")
	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 -를 제외한 11자리 숫자만 가능합니다.")
	String phoneNumber,

	@Schema(description = "사용자의 비밀번호", example = "password123!")
	@NotBlank(message = "비밀번호는 필수입니다.")
	String password,

	@Schema(description = "사용자의 이메일 주소", example = "hong@example.com")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email,

	@Schema(description = "생년월일, YYYY-MM-DD 형식으로 입력", example = "1990-01-01")
	@NotBlank(message = "생년월일은 필수입니다.")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해야 합니다.")
	String birthDate,

	@Schema(description = "성별 (M: 남성, W: 여성)", example = "M")
	@NotBlank(message = "성별은 필수입니다.")
	@Pattern(regexp = "^[MW]$", message = "성별은 M 또는 W여야 합니다.")
	String gender
) {
}
