package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.global.domain.BaseEntity;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.TrainSchedule;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(
	name = "seat_reservation",
	indexes = {
		@Index(name = "idx_seat_reservation_schedule", columnList = "train_schedule_id"),
		@Index(name = "idx_seat_reservation_section", columnList = "train_schedule_id, departure_station_id, arrival_station_id"),
		@Index(name = "idx_seat_reservation_seat", columnList = "train_schedule_id, seat_id")
	}
)
public class SeatReservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "seat_reservation_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id", nullable = false)
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id", nullable = true) // 입석일 경우 true
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Reservation reservation;

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_status", nullable = false)
	private SeatStatus seatStatus;

	private LocalDateTime reservedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id", nullable = false)
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id", nullable = false)
	private Station arrivalStation;

	@Column(name = "is_standing", nullable = false)
	private boolean isStanding = false;

	// 낙관적 락을 위한 필드
	@Version
	private Long version;

	/***
	 * 새로운 좌석 예약 현황을 생성하는 정적 메서드
	 * @param trainSchedule 열차 스케줄 엔티티
	 * @param seat 좌석 엔티티
	 * @return SeatReservation 엔티티
	 */
	public static SeatReservation createAvailable(TrainSchedule trainSchedule, Seat seat) {
		return SeatReservation.builder()
			.trainSchedule(trainSchedule)
			.seat(seat)
			.seatStatus(SeatStatus.AVAILABLE)
			.build();
	}

	/***
	 * 좌석 예약 현황을 예약 상태로 변경하는 메서드
	 */
	public void reserveSeat() {
		if (this.seatStatus != SeatStatus.AVAILABLE) {
			throw new BusinessException(BookingError.SEAT_NOT_AVAILABLE);
		}
		this.seatStatus = SeatStatus.RESERVED;
		this.reservedAt = LocalDateTime.now();
	}

	/***
	 * 좌석 예약 현황을 선택 가능 상태로 변경하는 메셔드
	 */
	public void cancelReservation() {
		if (this.seatStatus != SeatStatus.RESERVED) {
			throw new BusinessException(BookingError.SEAT_NOT_RESERVED);
		}
		this.seatStatus = SeatStatus.AVAILABLE;
		this.reservedAt = null;
	}

	/***
	 * 좌석 예약 현황의 항목이 만료되었는지 확인하는 메서드
	 * @param expirationMinutes 만료 시간
	 * @return 만료 여부
	 */
	public boolean isExpired(Integer expirationMinutes) {
		if (this.seatStatus != SeatStatus.RESERVED) {
			return false;
		}
		return this.reservedAt.isBefore(LocalDateTime.now().minusMinutes(expirationMinutes));
	}
}
