package com.sudo.railo.global.success;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.NonNull;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType,
		@NonNull Class<? extends HttpMessageConverter<?>> converterType) {
		return !Void.TYPE.equals(returnType.getParameterType());
	}

	@Override
	public Object beforeBodyWrite(
		Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
		@NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
		@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {

		if (body instanceof SuccessResponse<?>) {
			response.setStatusCode(((SuccessResponse<?>)body).getStatus());
		}
		return body;
	}
}
