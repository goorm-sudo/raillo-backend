package com.sudo.railo.global.exception.error;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)

public class ErrorResponse {

	@Builder.Default
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime timestamp = LocalDateTime.now();

	private String errorCode;
	private String errorMessage;
	private Object details;

	// ErrorCode를 사용한 정적 팩토리 메서드
	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.errorCode(errorCode.getCode())
			.errorMessage(errorCode.getMessage())
			.build();
	}

	// ErrorCode와 details를 사용한 정적 팩토리 메서드
	public static ErrorResponse of(ErrorCode errorCode, Object details) {
		return ErrorResponse.builder()
			.errorCode(errorCode.getCode())
			.errorMessage(errorCode.getMessage())
			.details(details)
			.build();
	}

	// 필드 에러 리스트를 위한 정적 팩토리 메서드
	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
		Map<String, String> errorDetails = new HashMap<>();
		fieldErrors.forEach(error ->
			errorDetails.put(error.getField(), error.getDefaultMessage())
		);

		return ErrorResponse.builder()
			.errorCode(errorCode.getCode())
			.errorMessage(errorCode.getMessage())
			.details(errorDetails)
			.build();
	}

	// 직접 값을 설정하는 정적 팩토리 메서드
	public static ErrorResponse of(String errorCode, String errorMessage, Object details) {
		return ErrorResponse.builder()
			.errorCode(errorCode)
			.errorMessage(errorMessage)
			.details(details)
			.build();
	}
}
