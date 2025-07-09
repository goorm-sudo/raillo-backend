package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TrainJdbcRepositoryImpl implements TrainJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void bulkInsertTrains(List<Train> trains) {
		String sql = "INSERT INTO train (train_number, train_type, train_name, total_cars) VALUES (?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(sql, trains, trains.size(), (ps, train) -> {
			ps.setInt(1, train.getTrainNumber());
			ps.setString(2, train.getTrainType().name());
			ps.setString(3, train.getTrainName());
			ps.setInt(4, train.getTotalCars());
		});
	}

	@Override
	public void bulkInsertTrainCars(List<TrainCar> trainCars) {
		String sql = "INSERT INTO train_car (car_number, car_type, seat_row_count, total_seats, seat_arrangement, train_id) VALUES (?, ?, ?, ?, ?, ?)";
		int batchSize = 2000;

		for (int i = 0; i < trainCars.size(); i += batchSize) {
			int end = Math.min(i + batchSize, trainCars.size());
			List<TrainCar> batchList = trainCars.subList(i, end);

			jdbcTemplate.batchUpdate(sql, batchList, batchList.size(), (ps, trainCar) -> {
				ps.setInt(1, trainCar.getCarNumber());
				ps.setString(2, trainCar.getCarType().name());
				ps.setInt(3, trainCar.getSeatRowCount());
				ps.setInt(4, trainCar.getTotalSeats());
				ps.setString(5, trainCar.getSeatArrangement());
				ps.setLong(6, trainCar.getTrain().getId());
			});
		}
	}

	@Override
	public void bulkInsertSeats(List<Seat> seats) {
		String sql = "INSERT INTO seat (seat_row, seat_column, seat_type, is_accessible, is_available, train_car_id) VALUES (?, ?, ?, ?, ?, ?)";
		int batchSize = 10000;

		for (int i = 0; i < seats.size(); i += batchSize) {
			int end = Math.min(i + batchSize, seats.size());
			List<Seat> batchList = seats.subList(i, end);

			jdbcTemplate.batchUpdate(sql, batchList, batchList.size(), (ps, seat) -> {
				ps.setInt(1, seat.getSeatRow());
				ps.setString(2, seat.getSeatColumn());
				ps.setString(3, seat.getSeatType().name());
				ps.setString(4, seat.getIsAccessible());
				ps.setString(5, seat.getIsAvailable());
				ps.setLong(6, seat.getTrainCar().getId());
			});
		}
	}
}
