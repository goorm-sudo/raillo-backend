package com.sudo.railo.train.domain;

import static com.sudo.railo.train.config.TrainTemplateProperties.*;

import java.util.ArrayList;
import java.util.List;

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
	public static Train create(int trainNumber, TrainType trainType, String trainName, TrainTemplate template) {
		Train train = new Train(trainNumber, trainType, trainName, template.cars().size());

		for (int i = 0; i < template.cars().size(); i++) {
			int carNumber = i + 1;
			CarConfig config = template.cars().get(i);
			SeatLayout layout = template.layouts().get(config.carType());

			TrainCar trainCar = TrainCar.create(carNumber, layout, config);
			train.addTrainCar(trainCar);
		}
		return train;
	}

	/* 연관 관계 편의 메서드 */

	/**
	 * TrainCar 추가
	 * 읽기 전용(mappedBy 쪽)에서 편의 메서드(연관관계 편의 메서드)로 관계 설정(Train이 TrainCar를 소유)
	 */
	public void addTrainCar(TrainCar trainCar) {
		trainCars.add(trainCar);        // Train -> TrainCar 연결 (객체그래프)
		trainCar.setTrain(this);        // TrainCar -> Train 연결 (양방향, 외래키 설정)
	}

	/**
	 * TrainCar 제거
	 */
	public void removeTrainCar(TrainCar trainCar) {
		trainCars.remove(trainCar);
		trainCar.setTrain(null);
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
