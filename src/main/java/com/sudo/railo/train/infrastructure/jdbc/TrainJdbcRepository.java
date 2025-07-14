package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;

public interface TrainJdbcRepository {

	void saveAllTrains(List<Train> trains);

	void saveAllTrainCars(List<TrainCar> trainCars);

	void saveAllSeats(List<Seat> seats);
}
