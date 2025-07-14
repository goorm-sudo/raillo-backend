package com.sudo.railo.train.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.config.TrainTemplateProperties;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.infrastructure.TrainCarRepository;
import com.sudo.railo.train.infrastructure.jdbc.TrainJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainCarService {

	private final TrainTemplateProperties properties;
	private final TrainCarRepository trainCarRepository;
	private final TrainJdbcRepository trainJdbcRepository;
	private final SeatService seatService;

	/**
	 * 객차 생성
	 */
	public void createTrainCars(List<Train> trains) {
		// 객차 생성
		List<TrainCar> trainCars = trains.stream()
			.flatMap(train -> train.generateTrainCars(
				properties.getLayouts(),
				properties.getTrainTemplate(train)
			).stream())
			.toList();

		// 객차 저장
		trainJdbcRepository.saveAllTrainCars(trainCars);
		log.info("{}개의 객차 저장 완료", trainCars.size());

		// 좌석 생성
		seatService.createSeats(fetchTrainCars(trains));
	}

	/**
	 * 객차 ID를 가져오기 위한 메서드
	 */
	private List<TrainCar> fetchTrainCars(List<Train> trains) {
		return trainCarRepository.findByTrainIn(trains);
	}
}
