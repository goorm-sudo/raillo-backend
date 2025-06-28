package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.TrainSchedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id", nullable = false)
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id", nullable = false)
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id", nullable = false)
	private Station arrivalStation;

	// TODO: 예매번호 생성 방식 결정 필요, 임시 Nullable
	@Column
	private Long reservationNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TripType tripType;

	@Column(nullable = false)
	private int totalPassengers;

	@Column(nullable = false)
	private String passengerSummary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus reservationStatus;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Column(nullable = false)
	private LocalDateTime reservedAt;

	private LocalDateTime paidAt;

	private LocalDateTime cancelledAt;
}
