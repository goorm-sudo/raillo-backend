package com.sudo.railo.train.infrastructure.jdbc;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.TrainSchedule;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TrainScheduleJdbcRepositoryImpl implements TrainScheduleJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private static final int BATCH_SIZE = 100;

	@Override
	public void saveAll(List<TrainSchedule> trainSchedules) {
		String sql = "INSERT INTO train_schedule (schedule_name, operation_date, departure_time, arrival_time, "
			+ "operation_status, delay_minutes, train_id, departure_station_id, arrival_station_id) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		for (int i = 0; i < trainSchedules.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, trainSchedules.size());
			List<TrainSchedule> batchList = trainSchedules.subList(i, end);

			jdbcTemplate.batchUpdate(sql, batchList, batchList.size(), (ps, ts) -> {
				ps.setString(1, ts.getScheduleName());
				ps.setDate(2, Date.valueOf(ts.getOperationDate()));
				ps.setTime(3, Time.valueOf(ts.getDepartureTime()));
				ps.setTime(4, Time.valueOf(ts.getArrivalTime()));
				ps.setString(5, ts.getOperationStatus().name());
				ps.setInt(6, ts.getDelayMinutes());
				ps.setLong(7, ts.getTrain().getId());
				ps.setLong(8, ts.getDepartureStation().getId());
				ps.setLong(9, ts.getArrivalStation().getId());
			});
		}
	}
}
