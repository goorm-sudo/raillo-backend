package com.sudo.railo.train.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.ScheduleStop;

@Repository
public interface ScheduleStopRepository extends JpaRepository<ScheduleStop, Long> {

	Optional<ScheduleStop> findByTrainScheduleIdAndStationId(Long trainScheduleId, Long id);
}
