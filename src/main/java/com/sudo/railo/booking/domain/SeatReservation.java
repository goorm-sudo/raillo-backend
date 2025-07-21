package com.sudo.railo.booking.domain;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sudo.railo.global.domain.BaseEntity;
import com.sudo.railo.train.domain.Seat;
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
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "seat_reservation",
	indexes = {@Index(name = "idx_seat_reservation_seat", columnList = "train_schedule_id, seat_id")},
	uniqueConstraints = {@UniqueConstraint(columnNames = {"train_schedule_id", "seat_id"})}
)
public class SeatReservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "seat_reservation_id")
	@Comment("예약 상태 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_schedule_id", nullable = false)
	@Comment("운행 일정 ID")
	private TrainSchedule trainSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id")
	@Comment("좌석 ID")
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@Comment("예약 ID")
	private Reservation reservation;

	@Enumerated(EnumType.STRING)
	@Column(name = "passenger_type", nullable = false)
	@Comment("승객 유형")
	private PassengerType passengerType;

	@Version
	@Comment("테이블 버전")
	private Long version;

	public boolean isStanding() {
		return seat == null;
	}
}
