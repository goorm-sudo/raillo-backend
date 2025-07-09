package com.sudo.railo.train.infrastructure;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.ScheduleStop;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleStopJdbcRepositoryImpl implements ScheduleStopJdbcRepository {

	private final JdbcTemplate jdbcTemplate;
	private static final int BATCH_SIZE = 1000;

	@Override
	public void bulkInsert(List<ScheduleStop> scheduleStops) {
		String sql = "INSERT INTO schedule_stop (stop_order, arrival_time, departure_time, train_schedule_id, station_id) VALUES (?, ?, ?, ?, ?)";

		for (int i = 0; i < scheduleStops.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, scheduleStops.size());
			List<ScheduleStop> batchList = scheduleStops.subList(i, end);

			jdbcTemplate.batchUpdate(sql, batchList, batchList.size(), (ps, ss) -> {
				ps.setInt(1, ss.getStopOrder());
				ps.setObject(2, ss.getArrivalTime());
				ps.setObject(3, ss.getDepartureTime());
				ps.setLong(4, ss.getTrainSchedule().getId());
				ps.setLong(5, ss.getStation().getId());
			});
		}
	}
}
