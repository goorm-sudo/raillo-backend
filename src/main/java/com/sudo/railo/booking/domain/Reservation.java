package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sudo.railo.member.domain.Member;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long id;

	// TODO: 운행 일정 id

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	// TODO: 출발역 id

	// TODO: 도착역 id

	@Column(nullable = false)
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

	private LocalDateTime reservedAt;

	private LocalDateTime paidAt;

	private LocalDateTime cancelledAt;

	/**
	 * 결제 완료 처리
	 * RESERVED -> PAID 상태 변경
	 */
	public void markAsPaid() {
		if (this.reservationStatus != ReservationStatus.RESERVED) {
			throw new IllegalStateException("예약 상태가 RESERVED가 아닙니다: " + this.reservationStatus);
		}
		this.reservationStatus = ReservationStatus.PAID;
		this.paidAt = LocalDateTime.now();
	}

	/**
	 * 결제 취소 처리
	 * RESERVED -> CANCELLED 상태 변경
	 */
	public void markAsCancelled() {
		if (this.reservationStatus != ReservationStatus.RESERVED && 
			this.reservationStatus != ReservationStatus.PAID) {
			throw new IllegalStateException("취소 불가능한 예약 상태입니다: " + this.reservationStatus);
		}
		this.reservationStatus = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
	}

	/**
	 * 환불 처리
	 * PAID -> REFUNDED 상태 변경
	 */
	public void markAsRefunded() {
		if (this.reservationStatus != ReservationStatus.PAID) {
			throw new IllegalStateException("환불 불가능한 예약 상태입니다: " + this.reservationStatus);
		}
		this.reservationStatus = ReservationStatus.REFUNDED;
		this.cancelledAt = LocalDateTime.now();
	}

	/**
	 * 예약 만료 처리
	 * RESERVED -> EXPIRED 상태 변경
	 */
	public void markAsExpired() {
		if (this.reservationStatus != ReservationStatus.RESERVED) {
			throw new IllegalStateException("만료 처리 불가능한 예약 상태입니다: " + this.reservationStatus);
		}
		this.reservationStatus = ReservationStatus.EXPIRED;
	}
}
