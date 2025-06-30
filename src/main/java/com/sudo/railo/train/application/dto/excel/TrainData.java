package com.sudo.railo.train.application.dto.excel;

import com.sudo.railo.train.domain.type.TrainType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainData {

	private int trainNumber;
	private TrainType trainType;
	private String trainName;

	/**
	 * 정적 팩토리 메서드
	 *
	 * @param trainNumber 열차 번호
	 * @param trainName 열차 이름
	 */
	public static TrainData of(int trainNumber, String trainName) {
		return new TrainData(trainNumber, TrainType.fromName(trainName), trainName);
	}
}
