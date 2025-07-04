package com.sudo.railo.train.application.dto.excel;

import java.time.LocalDate;
import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainScheduleData {

	private String scheduleName;
	private LocalDate operationDate;
	private List<ScheduleStopData> scheduleStopData;
	private TrainData trainData;

	/**
	 * 정적 팩토리 메서드
	 *
	 * @param scheduleName 스케줄 이름
	 * @param operationDate 운행일
	 * @param scheduleStopData 정차역 목록
	 * @param trainData 열차 데이터
	 */
	public static TrainScheduleData of(String scheduleName, LocalDate operationDate,
		List<ScheduleStopData> scheduleStopData, TrainData trainData) {

		return new TrainScheduleData(scheduleName, operationDate, scheduleStopData, trainData);
	}

	public ScheduleStopData getFirstStop() {
		validateScheduleStopData();
		return scheduleStopData.get(0);
	}

	public ScheduleStopData getLastStop() {
		validateScheduleStopData();
		return scheduleStopData.get(scheduleStopData.size() - 1);
	}

	private void validateScheduleStopData() {
		if (CollectionUtils.isEmpty(scheduleStopData)) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}
	}
}
