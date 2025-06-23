package com.sudo.railo.train.domain;

import java.time.LocalDateTime;

import com.sudo.railo.booking.domain.ReservationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "seat_reservation",
	indexes = {
		@Index(name = "idx_seat_reservation_schedule", columnList = "train_schedule_id"),
		@Index(name = "idx_seat_reservation_section", columnList = "train_schedule_id, departure_station_id, arrival_station_id"),
		@Index(name = "idx_seat_reservation_seat", columnList = "train_schedule_id, seat_id")
	}
)
public class SeatReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "train_schedule_id", nullable = false)
	private Long trainScheduleId;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	@Column(name = "departure_station_id", nullable = false)
	private Long departureStationId;

	@Column(name = "arrival_station_id", nullable = false)
	private Long arrivalStationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status;

	@Column(name = "reserved_at", nullable = false)
	private LocalDateTime reservedAt;

	public static SeatReservation create(Long trainScheduleId, Long seatId, Long reservationId,
		Long departureStationId, Long arrivalStationId) {
		SeatReservation seatReservation = new SeatReservation();
		seatReservation.trainScheduleId = trainScheduleId;
		seatReservation.seatId = seatId;
		seatReservation.reservationId = reservationId;
		seatReservation.departureStationId = departureStationId;
		seatReservation.arrivalStationId = arrivalStationId;
		seatReservation.status = ReservationStatus.RESERVED;
		seatReservation.reservedAt = LocalDateTime.now();
		return seatReservation;
	}

	public void cancel() {
		this.status = ReservationStatus.CANCELLED;
	}
}
