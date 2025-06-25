package com.sudo.railo.train.application.dto.response;

import org.springframework.http.HttpStatus;

import com.sudo.railo.global.success.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainScheduleSuccess implements SuccessCode {

	OPERATION_CALENDAR_SUCCESS(HttpStatus.OK, "운행 캘린더 조회가 완료되었습니다."),
	SCHEDULE_SEARCH_SUCCESS(HttpStatus.OK, "열차 스케줄 검색이 완료되었습니다.");

	private final HttpStatus status;
	private final String message;
}
