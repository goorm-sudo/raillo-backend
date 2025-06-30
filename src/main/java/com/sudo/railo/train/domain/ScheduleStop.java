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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "schedule_stop",
	indexes = {
		// 1. 경로 검색 최적화 인덱스
		@Index(name = "idx_schedule_stop_route",
			columnList = "station_id, departure_time, train_schedule_id, stop_order"),

		// 2. FK 조인 성능용 인덱스
		@Index(name = "idx_schedule_stop_schedule_fk",
			columnList = "train_schedule_id"),

		@Index(name = "idx_schedule_stop_station_fk",
			columnList = "station_id"),
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id")
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id")
	private Station station;

	private ScheduleStop(int stopOrder, LocalTime arrivalTime, LocalTime departureTime, Station station) {
		this.stopOrder = stopOrder;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.station = station;
	}

	/* 정적 팩토리 메서드 */
	public static ScheduleStop create(int stopOrder, LocalTime arrivalTime, LocalTime departureTime, Station station) {
		return new ScheduleStop(stopOrder, arrivalTime, departureTime, station);
	}

	/* 연관관계 편의 메서드 */
	public void setTrainSchedule(TrainSchedule trainSchedule) {
		this.trainSchedule = trainSchedule;
		trainSchedule.getScheduleStops().add(this);
	}
}
