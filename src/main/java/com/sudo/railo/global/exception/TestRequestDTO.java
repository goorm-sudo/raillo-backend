package com.sudo.railo.global.exception;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestRequestDTO {

	@NotBlank(message = "이름은 필수입니다")
	private String name;

	@Email(message = "올바른 이메일 형식이 아닙니다")
	@NotBlank(message = "이메일은 필수입니다")
	private String email;

	@NotNull(message = "나이는 필수입니다")
	@Positive(message = "나이는 양수여야 합니다")
	private Integer age;

	private String description;
}
