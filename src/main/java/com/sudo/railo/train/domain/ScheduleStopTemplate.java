package com.sudo.railo.train.domain;

import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleStopTemplate {

	@Id
	@UuidGenerator
	@Column(name = "schedule_stop_template_id")
	private UUID id;

	private int stopOrder;

	private LocalTime arrivalTime;

	private LocalTime departureTime;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id")
	private TrainScheduleTemplate trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id")
	private Station station;

	private ScheduleStopTemplate(
		int stopOrder,
		LocalTime arrivalTime,
		LocalTime departureTime,
		Station station
	) {
		this.stopOrder = stopOrder;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.station = station;
	}

	/**
	 * 정적 팩토리 메서드
	 */
	public static ScheduleStopTemplate create(
		int stopOrder,
		LocalTime arrivalTime,
		LocalTime departureTime,
		Station station
	) {
		return new ScheduleStopTemplate(stopOrder, arrivalTime, departureTime, station);
	}
}
