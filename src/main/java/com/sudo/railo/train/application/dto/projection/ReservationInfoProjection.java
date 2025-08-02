package com.sudo.railo.train.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.train.domain.type.CarType;

import lombok.Getter;

@Getter
public class ReservationInfoProjection {

	private final Long trainScheduleId;
	private final Long seatId;
	private final CarType carType;
	private final Long departureStationId;
	private final Long arrivalStationId;
	private final Boolean isStanding;

	@QueryProjection
	public ReservationInfoProjection(Long trainScheduleId, Long seatId, CarType carType,
		Long departureStationId, Long arrivalStationId, Boolean isStanding) {
		this.trainScheduleId = trainScheduleId;
		this.seatId = seatId;
		this.carType = carType;
		this.departureStationId = departureStationId;
		this.arrivalStationId = arrivalStationId;
		this.isStanding = isStanding;
	}
}
