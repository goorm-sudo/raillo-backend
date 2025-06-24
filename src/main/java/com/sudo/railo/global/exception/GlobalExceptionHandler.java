package com.sudo.railo.global.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.exception.error.GlobalError;
import com.sudo.railo.global.redis.RedisError;
import com.sudo.railo.global.security.TokenError;

import io.jsonwebtoken.io.SerializationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * @RequestBody 누락 처리 : HttpMessageNotReadableException
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
		HttpMessageNotReadableException e, HttpServletRequest request) {

		log.warn("Request body missing or invalid: {}", e.getMessage());

		return ResponseEntity.badRequest().body(
			ErrorResponse.of(
				"G_400",
				"요청 본문이 필요합니다. JSON 형식의 데이터를 포함해주세요.",
				Map.of(
					"path", "uri=" + request.getRequestURI(),
					"method", request.getMethod()
				)
			)
		);
	}

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
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
		MissingServletRequestParameterException ex) {
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

	/**
	 * 비밀번호 불일치 예외 처리
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
		ErrorResponse errorResponse = ErrorResponse.of(TokenError.INVALID_PASSWORD);
		log.warn("Bad credentials: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
	}

	/**
	 * Redis: 연결 실패 예외 처리
	 * */
	@ExceptionHandler(RedisConnectionFailureException.class)
	public ResponseEntity<ErrorResponse> handleRedisConnectionFailure(RedisConnectionFailureException ex) {
		ErrorResponse errorResponse = ErrorResponse.of(RedisError.REDIS_CONNECT_FAIL);
		log.warn("Redis connection failure: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * Redis: 직렬화 및 역직렬화 예외 처리
	 * */
	@ExceptionHandler(SerializationException.class)
	public ResponseEntity<ErrorResponse> handleRedisSerializationException(Exception ex) {
		ErrorResponse errorResponse = ErrorResponse.of(RedisError.SERIALIZATION_FAIL);
		log.warn("Redis serialization failure: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * Redis: 잘못된 API 사용 예외 처리
	 * */
	@ExceptionHandler(InvalidDataAccessApiUsageException.class)
	public ResponseEntity<ErrorResponse> handleRedisApiUsageException(InvalidDataAccessApiUsageException ex) {
		ErrorResponse errorResponse = ErrorResponse.of(RedisError.INVALID_DATA_ACCESS);
		log.warn("Redis API usage failure: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	/**
	 * 잘못된 인수 전달 시 예외 처리
	 * */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
		ErrorResponse errorResponse = ErrorResponse.of(GlobalError.INVALID_REQUEST_PARAM);
		log.warn("Invalid argument: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

}
