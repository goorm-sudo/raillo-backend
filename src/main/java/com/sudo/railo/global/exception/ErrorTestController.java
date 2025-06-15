package com.sudo.railo.global.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.exception.error.GlobalError;
import com.sudo.railo.member.exception.MemberError;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/test/errors")
@Validated
public class ErrorTestController {

	/**
	 * 1. @RequestBody 검증 실패 테스트
	 * POST /api/test/errors/validation
	 * Body: {"name": "", "email": "invalid-email", "age": -1}
	 */
	@PostMapping("/validation")
	public ResponseEntity<String> testValidation(@Valid @RequestBody TestRequestDTO request) {
		return ResponseEntity.ok("검증 성공: " + request.getName());
	}

	/**
	 * 2. @RequestParam 누락 테스트
	 * GET /api/test/errors/missing-param
	 * (필수 파라미터 name 없이 호출)
	 */
	@GetMapping("/missing-param")
	public ResponseEntity<String> testMissingParam(@RequestParam String name) {
		return ResponseEntity.ok("파라미터 받음: " + name);
	}

	/**
	 * 3. @RequestParam 검증 실패 테스트
	 * GET /api/test/errors/param-validation?age=abc
	 * GET /api/test/errors/param-validation?age=-5
	 */
	@GetMapping("/param-validation")
	public ResponseEntity<String> testParamValidation(
		@RequestParam @Min(value = 1, message = "나이는 1 이상이어야 합니다") Integer age) {
		return ResponseEntity.ok("나이: " + age);
	}

	/**
	 * 4. @PathVariable 검증 실패 테스트
	 * GET /api/test/errors/path-validation/invalid-id
	 * GET /api/test/errors/path-validation/123abc
	 */
	@GetMapping("/path-validation/{userId}")
	public ResponseEntity<String> testPathValidation(
		@PathVariable
		@Pattern(regexp = "^[0-9]+$", message = "사용자 ID는 숫자만 가능합니다")
		String userId) {
		return ResponseEntity.ok("사용자 ID: " + userId);
	}

	/**
	 * 5. GlobalError 비즈니스 예외 테스트
	 * GET /api/test/errors/global-error/{type}
	 */
	@GetMapping("/global-error/{type}")
	public ResponseEntity<String> testGlobalError(@PathVariable String type) {
		switch (type) {
			case "not-found":
				throw new BusinessException(GlobalError.RESOURCE_NOT_FOUND);
			case "already-exists":
				throw new BusinessException(GlobalError.RESOURCE_ALREADY_EXISTS);
			case "unauthorized":
				throw new BusinessException(GlobalError.UNAUTHORIZED_ACCESS);
			case "forbidden":
				throw new BusinessException(GlobalError.FORBIDDEN_ACCESS);
			case "database":
				throw new BusinessException(GlobalError.DATABASE_ERROR);
			default:
				return ResponseEntity.ok("지원하지 않는 타입: " + type);
		}
	}

	/**
	 * 6. MemberError 도메인 예외 테스트
	 * GET /api/test/errors/member-error/{type}
	 */
	@GetMapping("/member-error/{type}")
	public ResponseEntity<String> testMemberError(@PathVariable String type) {
		switch (type) {
			case "not-found":
				throw new BusinessException(MemberError.USER_NOT_FOUND);
			case "duplicate-email":
				throw new BusinessException(MemberError.DUPLICATE_EMAIL);
			case "invalid-password":
				throw new BusinessException(MemberError.INVALID_PASSWORD);
			default:
				return ResponseEntity.ok("지원하지 않는 타입: " + type);
		}
	}

	/**
	 * 7. 일반 RuntimeException 테스트
	 * GET /api/test/errors/runtime-exception
	 */
	@GetMapping("/runtime-exception")
	public ResponseEntity<String> testRuntimeException() {
		throw new RuntimeException("예상치 못한 런타임 에러 발생!");
	}

	/**
	 * 8. NullPointerException 테스트
	 * GET /api/test/errors/null-pointer
	 */
	@GetMapping("/null-pointer")
	public ResponseEntity<String> testNullPointerException() {
		String nullString = null;
		return ResponseEntity.ok(nullString.length() + ""); // NPE 발생
	}

	/**
	 * 9. 커스텀 메시지와 함께 비즈니스 예외 테스트
	 * GET /api/test/errors/custom-message
	 */
	@GetMapping("/custom-message")
	public ResponseEntity<String> testCustomMessage() {
		throw new BusinessException(MemberError.USER_NOT_FOUND, "사용자 ID 123을 찾을 수 없습니다");
	}

	/**
	 * 10. 예외 체이닝 테스트
	 * GET /api/test/errors/exception-chain
	 */
	@GetMapping("/exception-chain")
	public ResponseEntity<String> testExceptionChain() {
		try {
			// 의도적으로 예외 발생
			int result = 10 / 0;
			return ResponseEntity.ok("계산 결과: " + result);
		} catch (ArithmeticException e) {
			throw new BusinessException(GlobalError.INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * 11. 성공 케이스 (비교용)
	 * GET /api/test/errors/success?name=test&age=25
	 * POST /api/test/errors/success
	 * Body: {"name": "홍길동", "email": "hong@test.com", "age": 30}
	 */
	@GetMapping("/success")
	public ResponseEntity<Map<String, Object>> testSuccess(
		@RequestParam String name,
		@RequestParam @Min(1) Integer age) {
		return ResponseEntity.ok(Map.of(
			"message", "성공!",
			"name", name,
			"age", age,
			"timestamp", System.currentTimeMillis()
		));
	}

	@PostMapping("/success")
	public ResponseEntity<Map<String, Object>> testSuccessPost(@Valid @RequestBody TestRequestDTO request) {
		return ResponseEntity.ok(Map.of(
			"message", "검증 성공!",
			"data", request,
			"timestamp", System.currentTimeMillis()
		));
	}
}
