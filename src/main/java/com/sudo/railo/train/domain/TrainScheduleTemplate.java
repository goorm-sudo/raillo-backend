package com.sudo.railo.train.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainScheduleTemplate {

	@Id
	@UuidGenerator
	@Column(name = "train_schedule_template_id")
	private UUID id;

	private String scheduleName;

	private int operatingDay;

	private LocalTime departureTime;

	private LocalTime arrivalTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_id")
	private Train train;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id")
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id")
	private Station arrivalStation;

	@OneToMany(mappedBy = "trainSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ScheduleStopTemplate> scheduleStops = new ArrayList<>();

	private TrainScheduleTemplate(
		String scheduleName,
		int operatingDay,
		LocalTime departureTime,
		LocalTime arrivalTime,
		Train train,
		Station departureStation,
		Station arrivalStation
	) {
		this.scheduleName = scheduleName;
		this.operatingDay = operatingDay;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;

		this.train = train;
		this.departureStation = departureStation;
		this.arrivalStation = arrivalStation;
	}

	/**
	 * 정적 팩토리 메서드
	 */
	public static TrainScheduleTemplate create(
		String scheduleName,
		int operatingDay,
		LocalTime departureTime,
		LocalTime arrivalTime,
		Train train,
		Station departureStation,
		Station arrivalStation,
		List<ScheduleStopTemplate> scheduleStops
	) {
		TrainScheduleTemplate trainSchedule = new TrainScheduleTemplate(
			scheduleName,
			operatingDay,
			departureTime,
			arrivalTime,
			train,
			departureStation,
			arrivalStation
		);
		scheduleStops.forEach(trainSchedule::addScheduleStop);
		return trainSchedule;
	}

	public void addScheduleStop(ScheduleStopTemplate scheduleStop) {
		scheduleStops.add(scheduleStop);
		scheduleStop.setTrainSchedule(this);
	}
}
