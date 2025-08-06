package com.sudo.railo.support.helper;

import java.util.ArrayList;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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
	 * 테스트용 소형 열차 생성
	 * 객차 총 2개 (standard 1개 + firstClass 1개)
	 * 좌석 총 4개 (standard 24개 + firstClass 12개)
	 */
	@Transactional
	public Train createSmallTestTrain() {
		return createRealisticTrain(1, 1, 6, 4);
	}

	/**
	 * 테스트용 중형 열차 생성
	 * 객차 총 5개 (standard 3개 + firstClass 2개)
	 * 좌석 총 192개 (standard 144개 + firstClass 48개)
	 */
	@Transactional
	public Train createMediumTestTrain() {
		return createRealisticTrain(3, 2, 12, 8);
	}

	/**
	 * 테스트용 대형 열차 생성
	 * 객차 총 9개 (standard 6개 + firstClass 3개)
	 * 좌석 총 450개 (standard 360개 + firstClass 90개)
	 */
	@Transactional
	public Train createLargeTestTrain() {
		return createRealisticTrain(6, 3, 15, 10);
	}

	/**
	 * 특정 객차 타입별로 다른 행 수를 가진 복합 열차 생성
	 * 예: 앞쪽 일반실은 크고, 뒤쪽 일반실은 작게
	 * 총 280석 (일반실(2+2) 220석, 특실(2+1) 60석)
	 */
	@Transactional
	public Train createMixedConfigurationTrain() {
		Train train = createKTXTrain();
		Train savedTrain = trainRepository.save(train);

		List<CarSpec> carSpecs = List.of(
			new CarSpec(CarType.FIRST_CLASS, 12),  // 특실 1호차: 36석
			new CarSpec(CarType.STANDARD, 18),     // 일반실 2호차: 72석 (대형)
			new CarSpec(CarType.STANDARD, 15),     // 일반실 3호차: 60석 (중형)
			new CarSpec(CarType.STANDARD, 12),     // 일반실 4호차: 48석 (소형)
			new CarSpec(CarType.STANDARD, 10),     // 일반실 5호차: 40석 (소형)
			new CarSpec(CarType.FIRST_CLASS, 8)    // 특실 6호차: 24석
		);

		return saveTrainWithCarsAndSeats(savedTrain, carSpecs, createRealisticSeatLayouts());
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
	 * 고급 열차 생성 메서드 - 객차 개수와 각 객차별 행 수를 모두 설정 가능
	 *
	 * @param standardCarCount 일반실 객차 수
	 * @param firstClassCarCount 특실 객차 수
	 * @param standardRowsPerCar 일반실 객차당 행 수 (각 행은 4석 - 2+2 배치)
	 * @param firstClassRowsPerCar 특실 객차당 행 수 (각 행은 3석 - 2+1 배치)
	 * @return 생성된 열차
	 *
	 * 예시:
	 * - createAdvancedTrain(2, 1, 10, 6) → 일반실 2개(각 40석), 특실 1개(18석) = 총 98석
	 * - createAdvancedTrain(5, 3, 15, 10) → 일반실 5개(각 60석), 특실 3개(각 30석) = 총 390석
	 */
	@Transactional
	public Train createRealisticTrain(int standardCarCount, int firstClassCarCount,
		int standardRowsPerCar, int firstClassRowsPerCar) {

		// 입력값 검증
		validateTrainConfiguration(standardCarCount, firstClassCarCount, standardRowsPerCar, firstClassRowsPerCar);

		Train train = createKTXTrain(standardCarCount + firstClassCarCount);
		Train savedTrain = trainRepository.save(train);

		List<CarSpec> carSpecs = new ArrayList<>();

		// 일반실 객차들 추가
		for (int i = 0; i < standardCarCount; i++) {
			carSpecs.add(new CarSpec(CarType.STANDARD, standardRowsPerCar));
		}

		// 특실 객차들 추가
		for (int i = 0; i < firstClassCarCount; i++) {
			carSpecs.add(new CarSpec(CarType.FIRST_CLASS, firstClassRowsPerCar));
		}

		return saveTrainWithCarsAndSeats(savedTrain, carSpecs, createRealisticSeatLayouts());
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
	 * 주어진 seatId 목록에 해당하는 Seat 목록을 조회
	 */
	public List<Seat> getSeatsByIds(List<Long> seatIds) {
		return testSeatRepository.findAllById(seatIds);
	}

	/**
	 * 좌석 조회 메서드
	 * count만큼 carType에 해당하는 좌석 ID 조회
	 */
	public List<Long> getSeatIds(Train train, CarType carType, int count) {
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

	private Train createKTXTrain(int totalCars) {
		return Train.create(1, TrainType.KTX, "KTX", totalCars);
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

	private Train saveTrainWithCarsAndSeats(Train savedTrain, List<CarSpec> carSpecs,
		Map<CarType, SeatLayout> seatLayouts) {
		TrainTemplate trainTemplate = new TrainTemplate(carSpecs);

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

	/**
	 * 현실적인 좌석 배치 (실제 KTX와 동일)
	 * - 일반실: 4석/행 (2+2 배치)
	 * - 특실: 3석/행 (2+1 배치)
	 */
	private Map<CarType, SeatLayout> createRealisticSeatLayouts() {
		Map<CarType, SeatLayout> layouts = new HashMap<>();

		// 일반실: 2+2 배치 (AB 통로 CD) - 4석/행
		List<SeatColumn> standardColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE),
			new SeatColumn("C", SeatType.AISLE),
			new SeatColumn("D", SeatType.WINDOW)
		);
		layouts.put(CarType.STANDARD, new SeatLayout("2+2", standardColumns));

		// 특실: 2+1 배치 (AB 통로 C) - 3석/행
		List<SeatColumn> firstClassColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE),
			new SeatColumn("C", SeatType.WINDOW)
		);
		layouts.put(CarType.FIRST_CLASS, new SeatLayout("2+1", firstClassColumns));

		return layouts;
	}

	private void validateTrainConfiguration(int standardCarCount, int firstClassCarCount,
		int standardRowsPerCar, int firstClassRowsPerCar) {
		if (standardCarCount < 0 || firstClassCarCount < 0) {
			throw new IllegalArgumentException("객차 수는 0 이상이어야 합니다.");
		}
		if (standardCarCount == 0 && firstClassCarCount == 0) {
			throw new IllegalArgumentException("최소 하나의 객차는 있어야 합니다.");
		}
		if (standardRowsPerCar < 1 || firstClassRowsPerCar < 1) {
			throw new IllegalArgumentException("객차당 행 수는 1 이상이어야 합니다.");
		}
		if (standardCarCount + firstClassCarCount > 20) {
			throw new IllegalArgumentException("총 객차 수는 20개를 초과할 수 없습니다.");
		}

		// 총 좌석 수 제한 (너무 큰 테스트 데이터 방지)
		int totalSeats = (standardCarCount * standardRowsPerCar * 4) + (firstClassCarCount * firstClassRowsPerCar * 3);
		if (totalSeats > 1000) {
			log.warn("총 좌석 수가 {}석으로 너무 큽니다. 테스트 성능에 영향을 줄 수 있습니다.", totalSeats);
		}
	}
}
