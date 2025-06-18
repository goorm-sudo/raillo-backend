package com.sudo.railo.member.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
	@NotBlank
	String name,
	@NotBlank
	String phoneNumber,
	@NotBlank
	String password,
	@NotBlank
	String email,
	@NotBlank
	String birthDate,
	@NotBlank
	String gender
) {
}
