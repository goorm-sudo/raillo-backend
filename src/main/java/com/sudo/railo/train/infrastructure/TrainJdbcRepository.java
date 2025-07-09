package com.sudo.railo.train.infrastructure;

import java.util.List;

import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;

public interface TrainJdbcRepository {

	void bulkInsertTrains(List<Train> trains);

	void bulkInsertTrainCars(List<TrainCar> trainCars);

	void bulkInsertSeats(List<Seat> seats);
}
