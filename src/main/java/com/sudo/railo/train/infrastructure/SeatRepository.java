package com.sudo.railo.train.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.type.CarType;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	@Query("SELECT DISTINCT tc.carType FROM Seat s JOIN TrainCar tc ON tc = s.trainCar WHERE s.id IN :seatIds")
	List<CarType> findCarTypes(List<Long> seatIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Seat s WHERE s.id = :seatId")
	Optional<Seat> findByIdWithLock(Long seatId);
}
