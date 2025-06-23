package com.sudo.railo.train.application.dto;

import com.sudo.railo.train.domain.type.TrainType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainDto {

	private int trainNumber;
	private TrainType trainType;
	private String trainName;

	public static TrainDto of(int trainNumber, String trainName) {
		return new TrainDto(trainNumber, TrainType.fromName(trainName), trainName);
	}
}
