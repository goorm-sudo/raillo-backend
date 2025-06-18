package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(

	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 -를 제외한 11자리 숫자만 가능합니다.")
	String phoneNumber,

	@NotBlank(message = "비밀번호는 필수입니다.")
	String password,

	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email,

	@NotBlank(message = "생년월일은 필수입니다.")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해야 합니다.")
	String birthDate,

	@NotBlank(message = "성별은 필수입니다.")
	@Pattern(regexp = "^[MW]$", message = "성별은 M 또는 W여야 합니다.")
	String gender
) {
}
