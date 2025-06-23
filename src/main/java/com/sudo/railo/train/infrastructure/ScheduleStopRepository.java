package com.sudo.railo.train.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.application.dto.response.StationStopInfo;
import com.sudo.railo.train.domain.ScheduleStop;

@Repository
public interface ScheduleStopRepository extends JpaRepository<ScheduleStop, Long> {

	@Query("""
		SELECT new com.sudo.railo.train.application.dto.response.StationStopInfo(
		    ss.station.id, ss.station.stationName, ss.stopOrder, ss.arrivalTime, ss.departureTime
		)
		FROM ScheduleStop ss
		WHERE ss.trainSchedule.id = :trainScheduleId
		AND ss.stopOrder >= (SELECT ss1.stopOrder FROM ScheduleStop ss1 WHERE ss1.trainSchedule.id = :trainScheduleId AND ss1.station.id = :departureStationId)
		AND ss.stopOrder <= (SELECT ss2.stopOrder FROM ScheduleStop ss2 WHERE ss2.trainSchedule.id = :trainScheduleId AND ss2.station.id = :arrivalStationId)
		ORDER BY ss.stopOrder
		""")
	List<StationStopInfo> findStopsBetweenStations(@Param("trainScheduleId") Long trainScheduleId,
		@Param("departureStationId") Long departureStationId,
		@Param("arrivalStationId") Long arrivalStationId);
}
