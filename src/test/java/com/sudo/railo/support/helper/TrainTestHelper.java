package com.sudo.railo.support.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.support.repository.TestSeatRepository;
import com.sudo.railo.train.config.TrainTemplateProperties.CarSpec;
import com.sudo.railo.train.config.TrainTemplateProperties.SeatColumn;
import com.sudo.railo.train.config.TrainTemplateProperties.SeatLayout;
import com.sudo.railo.train.config.TrainTemplateProperties.TrainTemplate;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatType;
import com.sudo.railo.train.domain.type.TrainType;
import com.sudo.railo.train.infrastructure.TrainCarRepository;
import com.sudo.railo.train.infrastructure.TrainRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrainTestHelper {

	private final TrainRepository trainRepository;
	private final TrainCarRepository trainCarRepository;
	private final TestSeatRepository testSeatRepository;

	/**
	 * 기본 기차 생성 메서드
	 * 객차 총 2개 (standard 1개 + firstClass 1개)
	 * 좌석 총 4개 (standard 2개 + firstClass 2개)
	 */
	@Transactional
	public Train createKTX() {
		return createCustomKTX(1, 1);
	}

	/**
	 * 커스텀 기차 생성 메서드
	 * 객차 총 2개 (standard 1개 + firstClass 1개)
	 * 좌석 = (standardRows * 2)개 + (firstRows * 2)개
	 */
	@Transactional
	public Train createCustomKTX(int standardRows, int firstRows) {
		Train train = createKTXTrain();
		Train savedTrain = trainRepository.save(train);

		List<CarSpec> carSpecs = List.of(
			new CarSpec(CarType.STANDARD, standardRows),
			new CarSpec(CarType.FIRST_CLASS, firstRows)
		);

		return saveTrainWithCarsAndSeats(savedTrain, carSpecs);
	}

	/**
	 * 좌석 조회 메서드
	 * count만큼 carType에 해당하는 좌석 조회
	 */
	public List<Seat> getSeats(Train train, CarType carType, int count) {
		Pageable limit = PageRequest.of(0, count);
		return testSeatRepository.findByTrainIdAndCarTypeWithTrainCarLimited(train.getId(), carType, limit)
			.stream()
			.toList();
	}

	/**
	 * 좌석 조회 메서드
	 * count만큼 carType에 해당하는 좌석 ID 조회
	 */
	public List<Long> getSeatIds(Train train, CarType carType, int count) {
		return getSeats(train, carType, count)
			.stream()
			.map(Seat::getId)
			.toList();
	}

	/**
	 * 기차에 존재하는 모든 좌석 조회 메서드
	 */
	public List<Long> getAllSeatIds(Train train) {
		return testSeatRepository.findByTrainIdWithTrainCar(train.getId())
			.stream()
			.map(Seat::getId)
			.toList();
	}

	private Train createKTXTrain() {
		return Train.create(1, TrainType.KTX, "KTX", 2);
	}

	private Train saveTrainWithCarsAndSeats(Train savedTrain, List<CarSpec> carSpecs) {
		TrainTemplate trainTemplate = new TrainTemplate(carSpecs);
		Map<CarType, SeatLayout> seatLayouts = createSeatLayouts();

		List<TrainCar> trainCars = savedTrain.generateTrainCars(seatLayouts, trainTemplate);
		List<TrainCar> savedTrainCars = trainCarRepository.saveAll(trainCars);

		savedTrainCars.forEach(trainCar -> {
			CarSpec carSpec = getCarSpecByCarNumber(carSpecs, trainCar.getCarNumber());
			SeatLayout seatLayout = seatLayouts.get(trainCar.getCarType());
			List<Seat> seats = trainCar.generateSeats(carSpec, seatLayout);
			testSeatRepository.saveAll(seats);
		});

		return savedTrain;
	}

	private CarSpec getCarSpecByCarNumber(List<CarSpec> carSpecs, int carNumber) {
		return carSpecs.get(carNumber - 1);
	}

	private Map<CarType, SeatLayout> createSeatLayouts() {
		Map<CarType, SeatLayout> layouts = new HashMap<>();

		List<SeatColumn> standardColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE)
		);
		layouts.put(CarType.STANDARD, new SeatLayout("2+2", standardColumns));

		List<SeatColumn> firstClassColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE)
		);
		layouts.put(CarType.FIRST_CLASS, new SeatLayout("2+1", firstClassColumns));

		return layouts;
	}
}
