package com.sudo.railo.train.application.dto.response;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainScheduleSuccess implements SuccessCode {

	OPERATION_CALENDAR_SUCCESS(HttpStatus.OK, "운행 캘린더 조회가 완료되었습니다."),
	TRAIN_SEARCH_SUCCESS(HttpStatus.OK, "열차 조회가 완료되었습니다."),
	TRAIN_DETAIL_SUCCESS(HttpStatus.OK, "열차 상세 정보 조회가 완료되었습니다."),
	SEAT_AVAILABILITY_SUCCESS(HttpStatus.OK, "좌석 현황 조회가 완료되었습니다.");

	private final HttpStatus status;
	private final String message;
}
