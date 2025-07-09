package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import com.sudo.railo.train.domain.ScheduleStop;

public interface ScheduleStopJdbcRepository {

	void bulkInsert(List<ScheduleStop> scheduleStops);
}
