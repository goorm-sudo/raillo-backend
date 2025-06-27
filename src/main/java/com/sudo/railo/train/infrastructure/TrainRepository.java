package com.sudo.railo.train.infrastructure;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.Train;

public interface TrainRepository extends JpaRepository<Train, Long> {

	List<Train> findByTrainNumberIn(Collection<Integer> trainNumbers);
}
