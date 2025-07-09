package com.sudo.railo.train.infrastructure;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.train.domain.Train;

public interface TrainRepository extends JpaRepository<Train, Long> {

	@Query("SELECT t FROM Train t JOIN FETCH t.trainCars")
	List<Train> findAllWithCars();

	List<Train> findByTrainNumberIn(Collection<Integer> trainNumbers);
}
