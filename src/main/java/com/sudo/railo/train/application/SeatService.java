package com.sudo.railo.train.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.config.TrainTemplateProperties;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.infrastructure.TrainJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

	private final TrainTemplateProperties properties;
	private final TrainJdbcRepository trainJdbcRepository;

	/**
	 * 좌석 생성
	 */
	public void createSeats(List<TrainCar> trainCars) {
		// 좌석 생성
		List<Seat> seats = trainCars.stream()
			.flatMap(trainCar -> {
				TrainTemplateProperties.CarSpec spec = properties.getCarSpec(trainCar);
				TrainTemplateProperties.SeatLayout layout = properties.getSeatLayout(spec);
				return trainCar.generateSeats(spec, layout).stream();
			}).toList();

		// 좌석 저장
		trainJdbcRepository.bulkInsertSeats(seats);
		log.info("{}개의 좌석 저장 완료", seats.size());
	}
}
