package com.sudo.railo.train.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	name = "train_schedule",
	indexes = {
		// 1. 열차 예약 검색용 복합 인덱스 (날짜 + 출발역 + 도착역 + 운행상태 + 출발시간)
		// ex) 6월 20일 서울→부산 예약 가능한 열차 조회 (시간순 정렬)
		@Index(name = "idx_schedule_booking",
			columnList = "operation_date, departure_station_id, arrival_station_id, operation_status, departure_time"),

		// 2. 열차별 날짜 검색 (관리자용, 특정 열차 스케줄 조회)
		@Index(name = "idx_schedule_train_date",
			columnList = "train_id, operation_date"),

		// 3. 출발 시간 정렬 (같은 구간 내 시간순 조회)
		@Index(name = "idx_schedule_departure_time",
			columnList = "departure_time")
	}
)
public class TrainSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String scheduleName;

	private LocalDate operationDate;

	private LocalTime departureTime;

	private LocalTime arrivalTime;

	@Enumerated(EnumType.STRING)
	private OperationStatus operationStatus;

	private int delayMinutes;

	private int totalSeats;

	private int availableSeats;

	// TODO : Train은 unique가 있으면 안됨 (열차는 스케줄을 날마다 잡음)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_id")
	private Train train;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id")
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id")
	private Station arrivalStation;

	public static TrainSchedule create(
		String scheduleName,
		LocalDate operationDate,
		LocalTime departureTime,
		LocalTime arrivalTime,
		Train train,
		Station departureStation,
		Station arrivalStation,
		int totalSeats) {
		TrainSchedule schedule = new TrainSchedule();
		schedule.scheduleName = scheduleName;
		schedule.operationDate = operationDate;
		schedule.departureTime = departureTime;
		schedule.arrivalTime = arrivalTime;
		schedule.train = train;
		schedule.departureStation = departureStation;
		schedule.arrivalStation = arrivalStation;
		schedule.totalSeats = totalSeats;
		schedule.availableSeats = totalSeats; // 초기화
		schedule.operationStatus = OperationStatus.ACTIVE;
		schedule.delayMinutes = 0;

		return schedule;
	}

	/* 비즈니스 메서드 */
	public void updateOperationStatus(OperationStatus status) {
		this.operationStatus = status;
	}
}
