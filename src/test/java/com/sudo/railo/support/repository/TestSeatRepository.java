package com.sudo.railo.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.type.CarType;

/**
 * 테스트에서만 사용하는 Seat 조회 Repository
 */
public interface TestSeatRepository extends JpaRepository<Seat, Long> {

	@Query("SELECT s FROM Seat s JOIN FETCH s.trainCar tc JOIN FETCH tc.train t WHERE t.id = :trainId")
	List<Seat> findByTrainIdWithTrainCar(Long trainId);
	
	@Query("SELECT s FROM Seat s JOIN FETCH s.trainCar tc JOIN FETCH tc.train t WHERE t.id = :trainId AND tc.carType = :carType")
	List<Seat> findByTrainIdAndCarTypeWithTrainCar(Long trainId, CarType carType);
}