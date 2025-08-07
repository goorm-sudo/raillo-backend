package com.sudo.railo.train.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import com.sudo.railo.train.domain.status.OperationStatus;

import jakarta.persistence.Column;
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
		// 1. 열차 예약 검색용 복합 인덱스 (날짜 + 운행상태 + 출발시간)
		// ex) 6월 20일 예약 가능한 열차 조회 (시간순 정렬)
		@Index(name = "idx_schedule_basic_filter",
			columnList = "operation_date, operation_status, departure_time"),

		// 2. 캘린더 전용 인덱스 (날짜별 운행 여부 조회)
		@Index(name = "idx_schedule_calendar",
			columnList = "operation_date, operation_status"),

		// 3. 열차별 날짜 검색 (관리자용, 특정 열차 스케줄 조회)
		@Index(name = "idx_schedule_train_date",
			columnList = "train_id, operation_date"),
	}
)
public class TrainSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "train_schedule_id")
	private Long id;

	private String scheduleName;

	private LocalDate operationDate;

	private LocalTime departureTime;

	private LocalTime arrivalTime;

	@Enumerated(EnumType.STRING)
	private OperationStatus operationStatus;

	private int delayMinutes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_id")
	private Train train;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id")
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id")
	private Station arrivalStation;

	/* 생성 메서드 */

	/**
	 * private 생성자
	 */
	private TrainSchedule(
		String scheduleName,
		LocalDate operationDate,
		LocalTime departureTime,
		LocalTime arrivalTime,
		Train train,
		Station departureStation,
		Station arrivalStation) {

		this.scheduleName = scheduleName;
		this.operationDate = operationDate;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.operationStatus = OperationStatus.ACTIVE;
		this.delayMinutes = 0;

		// 연관관계 설정
		this.train = train;
		this.departureStation = departureStation;
		this.arrivalStation = arrivalStation;
	}

	/**
	 * 정적 팩토리 메서드
	 */
	public static TrainSchedule create(LocalDate operationDate, TrainScheduleTemplate template) {

		return new TrainSchedule(
			template.getScheduleName(),
			operationDate,
			template.getDepartureTime(),
			template.getArrivalTime(),
			template.getTrain(),
			template.getDepartureStation(),
			template.getArrivalStation()
		);
	}

	/* 비즈니스 메서드 */
	public void updateOperationStatus(OperationStatus status) {
		this.operationStatus = status;
	}

	/**
	 * 열차 전체 지연 시간 추가 및 상태 업데이트
	 *
	 * 지연 상태 기준
	 * - 5분 미만: ACTIVE
	 * - 5분 이상: DELAYED
	 * - 20분 이상: 예약 시 지연 안내
	 */
	public void addDelay(int minutes) {
		this.delayMinutes += minutes;

		if (this.delayMinutes >= 5) {
			this.operationStatus = OperationStatus.DELAYED;
		}
	}

	public void recoverDelay() {
		this.delayMinutes = 0;
		this.operationStatus = OperationStatus.ACTIVE;
	}
}
