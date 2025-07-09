package com.sudo.railo.train.infrastructure;

import java.util.List;

import com.sudo.railo.train.domain.TrainSchedule;

public interface TrainScheduleJdbcRepository {

	void bulkInsert(List<TrainSchedule> trainSchedules);
}
