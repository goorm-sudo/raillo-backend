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
	public static TrainCar create(int carNumber, SeatLayout layout, CarSpec cars) {
		int totalSeats = cars.row() * layout.columns().size();
		TrainCar trainCar = new TrainCar(carNumber, cars.carType(), totalSeats, layout.seatArrangement());

		for (int i = 1; i <= cars.row(); i++) {
			for (SeatColumn column : layout.columns()) {
				Seat seat = Seat.create(i, column.name(), column.seatType());
				trainCar.addSeat(seat);
			}
		}
		return trainCar;
	}

	/* 연관관계 편의 메서드 */
	public void setTrain(Train train) {
		this.train = train;
		train.getTrainCars().add(this);
	}

	public void addSeat(Seat seat) {
		seats.add(seat);
		seat.setTrainCar(this);
	}
}
