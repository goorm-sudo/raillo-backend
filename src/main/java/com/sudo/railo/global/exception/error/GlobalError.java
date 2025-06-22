package com.sudo.railo.global.exception.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalError implements ErrorCode {

	// 4xx 클라이언트 에러
	INVALID_REQUEST_PARAM("요청 파라미터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST, "G_001"),
	MISSING_REQUEST_PARAM("필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST, "G_002"),
	INVALID_REQUEST_BODY("요청 본문이 유효하지 않습니다.", HttpStatus.BAD_REQUEST, "G_003"),
	RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "G_004"),
	RESOURCE_ALREADY_EXISTS("이미 존재하는 리소스입니다.", HttpStatus.CONFLICT, "G_005"),
	UNAUTHORIZED_ACCESS("인증이 필요합니다.", HttpStatus.UNAUTHORIZED, "G_006"),
	FORBIDDEN_ACCESS("접근 권한이 없습니다.", HttpStatus.FORBIDDEN, "G_007"),
	METHOD_NOT_ALLOWED("허용되지 않은 HTTP 메소드입니다.", HttpStatus.METHOD_NOT_ALLOWED, "G_008"),
	INVALID_YN_VALUE("Y 또는 N 값만 허용됩니다.", HttpStatus.BAD_REQUEST, "G_009"),

	// 5xx 서버 에러
	INTERNAL_SERVER_ERROR("내부 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "G_500"),
	DATABASE_ERROR("데이터베이스 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "G_501"),
	EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "G_502");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
