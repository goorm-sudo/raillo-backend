package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import com.sudo.railo.global.domain.BaseEntity;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.train.domain.ScheduleStop;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long id; // 예약 ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id", nullable = false)
	private TrainSchedule trainSchedule; // 운행 일정 ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member; // 멤버 ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_stop_id", nullable = false)
	private ScheduleStop departureStop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_stop_id", nullable = false)
	private ScheduleStop arrivalStop;

	@Column(nullable = false)
	private String reservationCode; // 고객용 예매 코드

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TripType tripType; // 여행 타입

	@Column(nullable = false)
	private int totalPassengers; // 총 승객 수

	@Column(nullable = false)
	private String passengerSummary; // 유형 별 승객 수

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus reservationStatus; // 예약 상태

	@Column(nullable = false)
	private LocalDateTime expiresAt; // 만료 시간

	private LocalDateTime purchaseAt; // 결제 완료 시간

	private LocalDateTime cancelledAt; // 반환 시간

	@Column(nullable = false)
	private int fare;
}
