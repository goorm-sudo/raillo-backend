package com.sudo.railo.train.application.dto.response;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainSearchSuccess implements SuccessCode {

	OPERATION_CALENDAR_SUCCESS(HttpStatus.OK, "운행 캘린더 조회가 완료되었습니다."),
	TRAIN_SEARCH_SUCCESS(HttpStatus.OK, "열차 조회가 완료되었습니다."),
	TRAIN_DETAIL_SUCCESS(HttpStatus.OK, "열차 상세 정보 조회가 완료되었습니다."),
	TRAIN_CAR_LIST_SUCCESS(HttpStatus.OK, "열차 객차 목록 조회가 완료되었습니다."),
	TRAIN_CAR_SEAT_DETAIL_SUCCESS(HttpStatus.OK, "열차 객차 좌석 상세 조회가 완료되었습니다.");

	private final HttpStatus status;
	private final String message;
}
