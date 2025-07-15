package com.sudo.railo.train.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
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

	// 마일리지 시스템용 추가 필드들
	@Column(name = "actual_arrival_time")
	private LocalDateTime actualArrivalTime;

	@Column(name = "mileage_processed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean mileageProcessed = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_id")
	private Train train;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id")
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id")
	private Station arrivalStation;

	// 좌석 타입별 잔여 좌석 수 (Map 형태로 저장)
	@ElementCollection
	@CollectionTable(name = "schedule_available_seats",
		joinColumns = @JoinColumn(name = "train_schedule_id"))
	@MapKeyColumn(name = "car_type")
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "available_seats")
	private Map<CarType, Integer> availableSeatsMap = new HashMap<>();

	@OneToMany(mappedBy = "trainSchedule", cascade = CascadeType.ALL)
	private final List<ScheduleStop> scheduleStops = new ArrayList<>();

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

		// 초기 실제 도착시간 설정 (예정 시간으로)
		this.actualArrivalTime = operationDate.atTime(arrivalTime);
		this.mileageProcessed = false;

		// 연관관계 설정
		this.train = train;
		this.departureStation = departureStation;
		this.arrivalStation = arrivalStation;

		// 초기 좌석 수 설정
		initializeAvailableSeats();
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

	/** 초기 좌석 수 설정 */
	private void initializeAvailableSeats() {
		for (CarType carType : train.getSupportedCarTypes()) {
			int totalSeats = train.getTotalSeatsByType(carType);
			availableSeatsMap.put(carType, totalSeats);
		}
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

		// 실제 도착시간도 지연 시간만큼 업데이트
		this.actualArrivalTime = operationDate.atTime(arrivalTime).plusMinutes(this.delayMinutes);
	}

	public void recoverDelay() {
		this.delayMinutes = 0;
		this.operationStatus = OperationStatus.ACTIVE;
		
		// 실제 도착시간을 원래 예정 시간으로 복구
		this.actualArrivalTime = operationDate.atTime(arrivalTime);
	}

	/* 조회 로직 */

	// 특정 타입 잔여 좌석 수 조회
	public int getAvailableSeats(CarType carType) {
		return availableSeatsMap.getOrDefault(carType, 0);
	}

	// 특정 타입 총 좌석 수 조회
	public int getTotalSeats(CarType carType) {
		return train.getTotalSeatsByType(carType);
	}

	// 예약 가능 여부 확인
	public boolean canReserveSeats(CarType carType, int seatCount) {
		if (!isOperational())
			return false;
		if (!train.getSupportedCarTypes().contains(carType))
			return false;
		return getAvailableSeats(carType) >= seatCount;
	}

	// 좌석 가용성 상태 확인
	public SeatAvailabilityStatus getSeatAvailabilityStatus(CarType carType) {
		int available = getAvailableSeats(carType);

		if (available == 0) {
			return SeatAvailabilityStatus.SOLD_OUT;
		} else if (available <= 5) {
			return SeatAvailabilityStatus.FEW_REMAINING;
		} else if (available <= 10) {
			return SeatAvailabilityStatus.LIMITED;
		} else {
			return SeatAvailabilityStatus.AVAILABLE;
		}
	}

	// 운행 가능 여부
	public boolean isOperational() {
		return operationStatus == OperationStatus.ACTIVE ||
			operationStatus == OperationStatus.DELAYED;
	}

	// 소요 시간 계산
	public Duration getTravelDuration() {
		return Duration.between(departureTime, arrivalTime);
	}

	/* 검증 로직 */

	// 좌석 예약 검증
	private void validateSeatReservation(CarType carType, int seatCount) {
		if (seatCount <= 0) {
			throw new IllegalArgumentException("좌석 수는 1 이상이어야 합니다");
		}

		if (!isOperational()) {
			throw new IllegalStateException("운행이 중단된 열차입니다");
		}

		if (!train.getSupportedCarTypes().contains(carType)) {
			throw new IllegalArgumentException("지원하지 않는 좌석 타입입니다: " + carType);
		}

		if (getAvailableSeats(carType) < seatCount) {
			throw new IllegalStateException(
				"좌석이 부족합니다. 요청: " + seatCount + "석, 잔여: " + getAvailableSeats(carType) + "석");
		}
	}

	/* 마일리지 시스템용 메서드들 */

	/**
	 * 관리자가 수동으로 실제 도착시간을 설정
	 * @param actualArrivalTime 실제 도착한 시간
	 */
	public void setActualArrivalTime(LocalDateTime actualArrivalTime) {
		this.actualArrivalTime = actualArrivalTime;
		
		// 지연 시간 자동 계산
		LocalDateTime scheduledArrival = operationDate.atTime(arrivalTime);
		if (actualArrivalTime.isAfter(scheduledArrival)) {
			Duration delay = Duration.between(scheduledArrival, actualArrivalTime);
			this.delayMinutes = (int) delay.toMinutes();
			
			if (this.delayMinutes >= 5) {
				this.operationStatus = OperationStatus.DELAYED;
			}
		}
	}

	/**
	 * 마일리지 처리 완료 표시
	 */
	public void markMileageProcessed() {
		this.mileageProcessed = true;
	}

	/**
	 * 마일리지 처리 준비됨 여부 확인
	 * @param currentTime 현재 시간
	 * @return 마일리지 처리 가능 여부
	 */
	public boolean isReadyForMileageProcessing(LocalDateTime currentTime) {
		return actualArrivalTime != null 
			&& currentTime.isAfter(actualArrivalTime) 
			&& !mileageProcessed;
	}

	/**
	 * 중요한 지연 여부 확인 (20분 이상)
	 * @return 20분 이상 지연 여부
	 */
	public boolean hasSignificantDelay() {
		return delayMinutes >= 20;
	}

	/**
	 * 지연 분 반환
	 * @return 지연 시간(분)
	 */
	public int getDelayMinutes() {
		return delayMinutes;
	}

	/**
	 * 실제 도착시간 반환
	 * @return 실제 도착시간
	 */
	public LocalDateTime getActualArrivalTime() {
		return actualArrivalTime;
	}
}
