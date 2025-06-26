package com.sudo.railo.train.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.TrainSchedule;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {

}
