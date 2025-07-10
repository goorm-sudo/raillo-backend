package com.sudo.railo.train.application.dto.excel;

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
	private int operatingDay;
	private List<ScheduleStopData> scheduleStopData;
	private TrainData trainData;

	/**
	 * 정적 팩토리 메서드
	 *
	 * @param scheduleName 스케줄 이름
	 * @param operatingDay 운행일
	 * @param scheduleStopData 정차역 목록
	 * @param trainData 열차 데이터
	 */
	public static TrainScheduleData of(String scheduleName, int operatingDay,
		List<ScheduleStopData> scheduleStopData, TrainData trainData) {

		if (CollectionUtils.isEmpty(scheduleStopData)) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}

		return new TrainScheduleData(scheduleName, operatingDay, scheduleStopData, trainData);
	}

	public ScheduleStopData getFirstStop() {
		return scheduleStopData.get(0);
	}

	public ScheduleStopData getLastStop() {
		return scheduleStopData.get(scheduleStopData.size() - 1);
	}
}
