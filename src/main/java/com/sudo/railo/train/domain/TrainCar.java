package com.sudo.railo.train.domain;

import static com.sudo.railo.train.config.TrainTemplateProperties.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Comment;

import com.sudo.railo.train.domain.type.CarType;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainCar {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "train_car_id")
	private Long id;

	private int carNumber;

	@Enumerated(EnumType.STRING)
	private CarType carType;

	private int totalSeats;

	@Comment("2+2, 2+1")
	private String seatArrangement;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "train_id")
	private Train train;

	@OneToMany(mappedBy = "trainCar", cascade = CascadeType.ALL)
	private final List<Seat> seats = new ArrayList<>();

	/* 생성 메서드 */

	/**
	 * private 생성자
	 */
	private TrainCar(int carNumber, CarType carType, int totalSeats, String seatArrangement) {
		this.carNumber = carNumber;
		this.carType = carType;
		this.totalSeats = totalSeats;
		this.seatArrangement = seatArrangement;
	}

	/* 정적 팩토리 메서드 */
	public static TrainCar create(int carNumber, CarSpec spec, SeatLayout layout) {
		int totalSeats = spec.row() * layout.columns().size();
		return new TrainCar(carNumber, spec.carType(), totalSeats, layout.seatArrangement());
	}

	/**
	 * 객차의 좌석 생성
	 */
	public List<Seat> generateSeats(CarSpec spec, SeatLayout layout) {
		List<Seat> seats = new ArrayList<>();

		// 좌석 행 (1, 2, 3, 4)
		for (int row = 1; row <= spec.row(); row++) {

			// 좌석 열 문자 (A, B, C, D)
			for (SeatColumn column : layout.columns()) {

				// 좌석 생성
				Seat seat = Seat.create(row, column.name(), column.seatType());
				seats.add(seat);

				// 연관 관계 설정
				seat.setTrainCar(this);
			}
		}
		return seats;
	}
}
