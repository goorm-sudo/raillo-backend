package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestRegisterRequest(

	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 -를 제외한 11자리 숫자만 가능합니다.")
	String phoneNumber,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{5}$", message = "비밀번호는 5자리 숫자만 가능합니다.")
	String password
) {
}
