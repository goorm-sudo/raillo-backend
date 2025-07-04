package com.sudo.railo.train.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.TrainCar;

public interface TrainCarRepository extends JpaRepository<TrainCar, Long> {
}
