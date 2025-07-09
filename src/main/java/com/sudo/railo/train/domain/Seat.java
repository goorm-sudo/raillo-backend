package com.sudo.railo.train.domain;

import org.hibernate.annotations.Comment;

import com.sudo.railo.train.domain.type.SeatType;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "seat_id")
	private Long id;

	@Comment("좌석 행 (1, 2, 3, 4)")
	private int seatRow;

	@Column(length = 1)
	@Comment("좌석 열 문자(A, B, C, D)")
	private String seatColumn;

	@Enumerated(EnumType.STRING)
	private SeatType seatType;

	@Column(length = 1)
	private String isAccessible;

	@Column(length = 1)
	private String isAvailable;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_car_id")
	private TrainCar trainCar;

	private Seat(int seatRow, String seatColumn, SeatType seatType) {
		this.seatRow = seatRow;
		this.seatColumn = seatColumn;
		this.seatType = seatType;
		this.isAccessible = "Y"; // TODO: 필요 시 구현 예정
		this.isAvailable = "Y"; // TODO: 필요 시 구현 예정
	}

	public static Seat create(int seatRow, String seatColumn, SeatType seatType) {
		return new Seat(seatRow, seatColumn, seatType);
	}
}
