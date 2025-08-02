package com.sudo.railo.train.application.dto.projection;

import java.util.Map;

import com.sudo.railo.train.domain.type.CarType;

import lombok.Getter;

@Getter
public class TrainSeatInfoBatch {

	private final Map<Long, Map<CarType, Integer>> seatsCountByCarType;
	private final Map<Long, Integer> totalSeatsCount;

	public TrainSeatInfoBatch(Map<Long, Map<CarType, Integer>> seatsCountByCarType,
		Map<Long, Integer> totalSeatsCount) {
		this.seatsCountByCarType = seatsCountByCarType;
		this.totalSeatsCount = totalSeatsCount;
	}

	public Map<CarType, Integer> getSeatsCountByCarType(Long trainScheduleId) {
		return seatsCountByCarType.getOrDefault(trainScheduleId, Map.of());
	}

	public Integer getTotalSeatsCount(Long trainScheduleId) {
		return totalSeatsCount.getOrDefault(trainScheduleId, 0);
	}
}
