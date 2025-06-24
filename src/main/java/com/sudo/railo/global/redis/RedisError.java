package com.sudo.railo.global.redis;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisError implements ErrorCode {

	REDIS_CONNECT_FAIL("Redis 서버 연결에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "R_001"),
	SERIALIZATION_FAIL("Json data 직렬화/역직렬화에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "R_002"),
	INVALID_DATA_ACCESS("잘못된 Redis API 사용입니다.", HttpStatus.BAD_REQUEST, "R_003");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
