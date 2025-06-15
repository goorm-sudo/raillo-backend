package com.sudo.railo.global.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.exception.error.GlobalError;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * @RequestBody 유효성 검사 실패 처리 : MethodArgumentNotValidException
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
		ErrorResponse errorResponse = ErrorResponse.of(GlobalError.INVALID_REQUEST_BODY, fieldErrors);

		log.warn("Validation failed: {}", errorResponse.getDetails());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * @RequestParam 누락 처리 : MissingServletRequestParameterException
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
		String detail = String.format("Required parameter '%s' is missing", ex.getParameterName());
		ErrorResponse errorResponse = ErrorResponse.of(GlobalError.MISSING_REQUEST_PARAM, detail);

		log.warn("Missing request parameter: {}", ex.getParameterName());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * @PathVariable, @RequestParam 유효성 검사 실패 처리 : ConstraintViolationException
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getConstraintViolations().forEach(violation -> {
			String fieldName = violation.getPropertyPath().toString();
			String message = violation.getMessage();
			errors.put(fieldName, message);
		});

		ErrorResponse errorResponse = ErrorResponse.of(GlobalError.INVALID_REQUEST_PARAM, errors);
		log.warn("Constraint violation: {}", errors);
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * 비즈니스 예외 처리
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
		ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode());

		logBusinessException(ex);
		return ResponseEntity.status(ex.getErrorCode().getStatus()).body(errorResponse);
	}

	/**
	 * 예상하지 못한 모든 예외 처리 : Exception
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
		ErrorResponse errorResponse = ErrorResponse.of(
			GlobalError.INTERNAL_SERVER_ERROR,
			Map.of("path", request.getDescription(false))
		);

		log.error("Unexpected error occurred", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * 비즈니스 예외 로깅
	 */
	private void logBusinessException(BusinessException ex) {
		if (ex.getErrorCode().getStatus().is5xxServerError()) {
			log.error("Business exception occurred", ex);
		} else {
			log.warn("Business exception occurred: {}", ex.getMessage());
		}
	}
}
