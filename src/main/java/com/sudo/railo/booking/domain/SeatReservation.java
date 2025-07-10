package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sudo.railo.global.domain.BaseEntity;
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
import jakarta.persistence.UniqueConstraint;
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
	},
	uniqueConstraints = {
		@UniqueConstraint(
			columnNames = {"train_schedule_id", "seat_id"}
		)
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
	@Column(name = "passenger_type", nullable = false)
	private PassengerType passengerType;

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
}
