package com.sudo.railo.train.domain;

import org.hibernate.annotations.Comment;

import com.sudo.railo.train.domain.type.CarType;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainCar {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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

	/* 생성 메서드 */
	public static TrainCar create(int carNumber, CarType carType, int totalSeats, String seatArrangement, Train train) {
		TrainCar trainCar = new TrainCar();
		trainCar.carNumber = carNumber;
		trainCar.carType = carType;
		trainCar.totalSeats = totalSeats;
		trainCar.seatArrangement = seatArrangement;
		trainCar.train = train;
		return trainCar;
	}

	/* 연관관계 편의 메서드 */
	public void setTrain(Train train) {
		this.train = train;
		train.getTrainCars().add(this);
	}
}
