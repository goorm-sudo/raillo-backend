package com.sudo.railo.train.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.train.domain.type.CarType;

import lombok.Getter;

@Getter
public class TrainSeatInfoProjection {

	private final Long trainScheduleId;
	private final CarType carType;
	private final Integer seatCount;

	@QueryProjection
	public TrainSeatInfoProjection(Long trainScheduleId, CarType carType, Integer seatCount) {
		this.trainScheduleId = trainScheduleId;
		this.carType = carType;
		this.seatCount = seatCount;
	}
}
