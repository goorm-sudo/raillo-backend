package com.sudo.railo.train.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "schedule_stop",
	indexes = {
		// 1. 출발역 필터 + 시간 조건 + 조인 및 stop_order 비교
		@Index(name = "idx_stop_depart_filter",
			columnList = "station_id, departure_time, train_schedule_id, stop_order"),

		// 2. 도착역 필터 + 조인 + stop_order 비교
		@Index(name = "idx_stop_arrival_filter",
			columnList = "station_id, train_schedule_id, stop_order"),
	}
)
public class ScheduleStop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_stop_id")
	private Long id;

	private int stopOrder;

	private LocalTime arrivalTime;

	private LocalTime departureTime;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id")
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id")
	private Station station;

	private ScheduleStop(int stopOrder, LocalTime arrivalTime, LocalTime departureTime,
		Station station, TrainSchedule trainSchedule) {

		this.stopOrder = stopOrder;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.station = station;
		this.trainSchedule = trainSchedule;
	}

	/* 정적 팩토리 메서드 */
	public static ScheduleStop create(ScheduleStopTemplate template, TrainSchedule trainSchedule) {

		return new ScheduleStop(
			template.getStopOrder(),
			template.getArrivalTime(),
			template.getDepartureTime(),
			template.getStation(),
			trainSchedule
		);
	}
}
