package com.sudo.railo.support.util;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.support.fixture.TrainFixture;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.infrastructure.SeatRepository;
import com.sudo.railo.train.infrastructure.TrainCarRepository;
import com.sudo.railo.train.infrastructure.TrainRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrainTestHelper {

	private final TrainRepository trainRepository;
	private final TrainCarRepository trainCarRepository;
	private final SeatRepository seatRepository;

	@Transactional
	public Train saveKTX() {
		return saveTrainWithCarsAndSeats(TrainFixture.KTX);
	}

	@Transactional
	public Train saveKTXSancheon() {
		return saveTrainWithCarsAndSeats(TrainFixture.KTX_SANCHEON);
	}

	@Transactional
	public Train saveKTXCheongryong() {
		return saveTrainWithCarsAndSeats(TrainFixture.KTX_CHEONGRYONG);
	}

	@Transactional
	public Train saveKTXEum() {
		return saveTrainWithCarsAndSeats(TrainFixture.KTX_EUM);
	}

	@Transactional
	public Train saveTrainWithCarsAndSeats(TrainFixture trainFixture) {
		Train train = trainFixture.create();
		Train savedTrain = trainRepository.save(train);

		List<TrainCar> trainCars = train.generateTrainCars(
			TrainFixture.createSeatLayouts(),
			trainFixture.createTrainTemplate()
		);
		List<TrainCar> savedTrainCars = trainCarRepository.saveAll(trainCars);

		savedTrainCars.stream().map(trainCar -> trainCar.generateSeats(
			trainFixture.getCarSpecByCarNumber(trainCar.getCarNumber()),
			TrainFixture.getSeatLayoutByCarType(trainCar.getCarType())
		)).forEach(seatRepository::saveAll);

		return savedTrain;
	}
}
