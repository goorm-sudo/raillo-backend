package com.sudo.railo.train.exception;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.exception.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainErrorCode implements ErrorCode {

	// 열차 스케줄 관련
	TRAIN_SCHEDULE_NOT_FOUND("해당 날짜에 운행하는 열차가 없습니다.", HttpStatus.NOT_FOUND, "T4001"),
	TRAIN_OPERATION_CANCELLED("해당 열차는 운행이 취소되었습니다.", HttpStatus.BAD_REQUEST, "T4002"),
	TRAIN_OPERATION_DELAYED("해당 열차는 지연 운행 중입니다.", HttpStatus.BAD_REQUEST, "T4003"),

	// 좌석 예약 관련
	SEAT_NOT_AVAILABLE("선택한 좌석을 예약할 수 없습니다.", HttpStatus.BAD_REQUEST, "T4101"),
	SEAT_ALREADY_RESERVED("이미 예약된 좌석입니다.", HttpStatus.CONFLICT, "T4102"),
	INSUFFICIENT_SEATS("요청한 승객 수만큼 좌석이 부족합니다.", HttpStatus.BAD_REQUEST, "T4103"),
	STANDING_NOT_AVAILABLE("입석 예약이 불가능합니다.", HttpStatus.BAD_REQUEST, "T4104"),
	TRAIN_SOLD_OUT("해당 열차는 완전 매진되었습니다.", HttpStatus.BAD_REQUEST, "T4105"),

	// 역 및 구간 관련
	STATION_NOT_FOUND("존재하지 않는 역입니다.", HttpStatus.NOT_FOUND, "T4201"),
	STATION_FARE_NOT_FOUND("해당 구간의 요금 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "T4202"),
	INVALID_ROUTE("출발역과 도착역이 동일하거나 유효하지 않은 경로입니다.", HttpStatus.BAD_REQUEST, "T4203"),

	// 승객 관련
	INVALID_PASSENGER_COUNT("승객 수는 1명 이상 9명 이하여야 합니다.", HttpStatus.BAD_REQUEST, "T4301"),
	PASSENGER_COUNT_EXCEEDS_LIMIT("한 번에 예약 가능한 최대 승객 수를 초과했습니다.", HttpStatus.BAD_REQUEST, "T4302"),

	// 날짜 관련
	INVALID_OPERATION_DATE("운행 날짜는 오늘 이후여야 합니다.", HttpStatus.BAD_REQUEST, "T4401"),
	OPERATION_DATE_TOO_FAR("예약 가능한 기간을 초과했습니다. (최대 1개월)", HttpStatus.BAD_REQUEST, "T4402"),
	NO_OPERATION_ON_DATE("해당 날짜와 운행하는 열차가 없습니다.", HttpStatus.NOT_FOUND, "T4403"),

	// 검색 관련
	NO_SEARCH_RESULTS("검색 조건에 맞는 열차가 없습니다.", HttpStatus.NOT_FOUND, "T4501"),
	INVALID_SEARCH_CONDITION("잘못된 검색 조건입니다.", HttpStatus.BAD_REQUEST, "T4502"),

	// 예약 관련
	RESERVATION_NOT_FOUND("예약 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, "T4601"),
	RESERVATION_ALREADY_CANCELLED("이미 취소된 예약입니다.", HttpStatus.BAD_REQUEST, "T4602"),
	RESERVATION_CANNOT_BE_CANCELLED("취소할 수 없는 예약입니다.", HttpStatus.BAD_REQUEST, "T4603"),

	// 시스템 관련
	TRAIN_SYSTEM_ERROR("열차 시스템 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, "T5001"),
	EXTERNAL_API_ERROR("외부 API 연동 중 오류가 발생했습니다.", HttpStatus.SERVICE_UNAVAILABLE, "T5002");

	private final String message;
	private final HttpStatus status;
	private final String code;
}
