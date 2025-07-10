package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import com.sudo.railo.train.domain.TrainSchedule;

public interface TrainScheduleJdbcRepository {

	void saveAll(List<TrainSchedule> trainSchedules);
}
