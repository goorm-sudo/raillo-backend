package com.sudo.railo.global.success;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SuccessResponse<T> {

	@JsonIgnore
	private final HttpStatus status;
	private final String message;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T result;

	public static SuccessResponse<?> of(SuccessCode successCode) {
		return new SuccessResponse<>(successCode.getStatus(), successCode.getMessage());
	}

	public static <T> SuccessResponse<T> of(SuccessCode successCode, T result) {
		return new SuccessResponse<>(successCode.getStatus(), successCode.getMessage(), result);
	}
}
