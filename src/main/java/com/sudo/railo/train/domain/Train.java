package com.sudo.railo.train.domain;

import static com.sudo.railo.train.config.TrainTemplateProperties.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Comment;

import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.TrainType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Train {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "train_id")
	private Long id;

	private int trainNumber;

	@Enumerated(EnumType.STRING)
	private TrainType trainType;

	@Comment("KTX, KTX-산천, ITX-청춘 등")
	private String trainName;

	private int totalCars;

	@OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
	private final List<TrainCar> trainCars = new ArrayList<>();

	/* 생성 메서드 */

	/**
	 * private 생성자
	 */
	private Train(int trainNumber, TrainType trainType, String trainName, int totalCars) {
		this.trainNumber = trainNumber;
		this.trainType = trainType;
		this.trainName = trainName;
		this.totalCars = totalCars;
	}

	/**
	 * 정적 팩토리 메서드
	 */
	public static Train create(int trainNumber, TrainType trainType, String trainName, int totalCars) {
		return new Train(trainNumber, trainType, trainName, totalCars);
	}

	/**
	 * 열차의 객차 생성
	 */
	public List<TrainCar> generateTrainCars(Map<CarType, SeatLayout> layouts, TrainTemplate template) {
		List<TrainCar> trainCars = new ArrayList<>();
		for (int i = 0; i < template.cars().size(); i++) {
			int carNumber = i + 1;

			CarSpec spec = template.cars().get(i);
			SeatLayout layout = layouts.get(spec.carType());

			// 객차 생성
			TrainCar trainCar = TrainCar.create(carNumber, spec, layout);
			trainCars.add(trainCar);

			// 연관 관계 설정
			trainCar.setTrain(this);
		}
		return trainCars;
	}

	/* 조회 로직 */

	/**
	 * 좌석 타입별 총 좌석 수 계산
	 */
	public int getTotalSeatsByType(CarType carType) {
		return trainCars.stream()
			.filter(car -> car.getCarType() == carType)
			.mapToInt(TrainCar::getTotalSeats)
			.sum();
	}

	/**
	 * 좌석 타입별 칸 목록
	 */
	public List<TrainCar> getCarsByType(CarType carType) {
		return trainCars.stream()
			.filter(car -> car.getCarType() == carType)
			.toList();
	}

	/**
	 * 지원하는 좌석 타입
	 */
	public List<CarType> getSupportedCarTypes() {
		return trainCars.stream()
			.map(TrainCar::getCarType)
			.distinct()
			.toList();
	}
}
