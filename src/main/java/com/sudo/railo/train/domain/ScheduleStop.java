package com.sudo.railo.train.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
