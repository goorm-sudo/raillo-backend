package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SeatReservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "seat_reservation_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id", nullable = false)
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
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

	// 낙관적 락을 위한 필드
	@Version
	private Long version;

	// 빈 좌석을 생성하는 메서드
	public static SeatReservation createAvailable(TrainSchedule trainSchedule, Seat seat) {
		return SeatReservation.builder()
			.trainSchedule(trainSchedule)
			.seat(seat)
			.seatStatus(SeatStatus.AVAILABLE)
			.build();
	}

	// 좌석을 예약하는 메서드
	public void reserveSeat() {
		if (this.seatStatus != SeatStatus.AVAILABLE) {
			throw new BusinessException(BookingError.SEAT_NOT_AVAILABLE);
		}
		this.seatStatus = SeatStatus.RESERVED;
		this.reservedAt = LocalDateTime.now();
	}

	// 좌석 예약을 취소하는 메서드
	public void cancelReservation() {
		if (this.seatStatus != SeatStatus.RESERVED) {
			throw new BusinessException(BookingError.SEAT_NOT_RESERVED);
		}
		this.seatStatus = SeatStatus.AVAILABLE;
		this.reservedAt = null;
	}

	// 좌석 예약이 만료되었는지 확인하는 메서드
	// 도메인 엔티티에서는 직접적으로 Spring의 설정을 주입받는것이 권장되지 않습니다.
	// 추후 서비스 레이어에서 이 메서드를 사용할때, 설정값을 가져와 매개변수로 사용합니다.
	public boolean isExpired(Integer expirationMinutes) {
		if (this.seatStatus != SeatStatus.RESERVED) {
			return false;
		}
		return this.reservedAt.isBefore(LocalDateTime.now().minusMinutes(expirationMinutes));
	}

}
