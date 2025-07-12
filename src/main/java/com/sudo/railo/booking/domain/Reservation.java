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

	@Column(nullable = false)
	private String reservationCode;

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

	/**
	 * 예약 상태를 PAID로 변경 (결제 완료 시)
	 */
	public void markAsPaid() {
		// 유효성 검증: RESERVED 상태에서만 PAID로 변경 가능
		if (this.reservationStatus != ReservationStatus.RESERVED) {
			throw new IllegalStateException(
				String.format("예약 상태가 RESERVED가 아닙니다. 현재 상태: %s (예약ID: %d)", 
					this.reservationStatus, this.id)
			);
		}
		
		this.reservationStatus = ReservationStatus.PAID;
		this.paidAt = LocalDateTime.now();
	}

	/**
	 * 예약 상태를 CANCELLED로 변경 (결제 취소 시)
	 */
	public void markAsCancelled() {
		// 유효성 검증: RESERVED 상태에서만 CANCELLED로 변경 가능
		if (this.reservationStatus != ReservationStatus.RESERVED) {
			throw new IllegalStateException(
				String.format("예약 상태가 RESERVED가 아닙니다. 현재 상태: %s (예약ID: %d)", 
					this.reservationStatus, this.id)
			);
		}
		
		this.reservationStatus = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
	}

	/**
	 * 예약 상태를 REFUNDED로 변경 (환불 완료 시)
	 */
	public void markAsRefunded() {
		// 유효성 검증: PAID 상태에서만 REFUNDED로 변경 가능
		if (this.reservationStatus != ReservationStatus.PAID) {
			throw new IllegalStateException(
				String.format("예약 상태가 PAID가 아닙니다. 현재 상태: %s (예약ID: %d)", 
					this.reservationStatus, this.id)
			);
		}
		
		this.reservationStatus = ReservationStatus.REFUNDED;
		this.cancelledAt = LocalDateTime.now();
	}
}
