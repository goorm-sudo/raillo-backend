package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.excel.TrainData;
import com.sudo.railo.train.config.TrainTemplateProperties;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.infrastructure.TrainRepository;
import com.sudo.railo.train.infrastructure.jdbc.TrainJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {

	private final TrainTemplateProperties properties;
	private final TrainRepository trainRepository;
	private final TrainJdbcRepository trainJdbcRepository;
	private final TrainCarService trainCarService;

	@Transactional(readOnly = true)
	public Map<Integer, Train> getTrainMap() {
		return trainRepository.findAllWithCars().stream()
			.collect(Collectors.toMap(Train::getTrainNumber, Function.identity()));
	}

	/**
	 * 열차 조회 및 저장
	 */
	@Transactional
	public Map<Integer, Train> findOrCreateTrains(List<TrainData> trainData) {
		Map<Integer, Train> trainMap = findExistingTrains();

		// 열차 생성
		List<Train> trains = trainData.stream()
			.filter(data -> !trainMap.containsKey(data.getTrainNumber()))
			.map(data -> Train.create(
				data.getTrainNumber(),
				data.getTrainType(),
				data.getTrainName(),
				properties.getTemplates().get(data.getTrainType()).cars().size()
			)).toList();

		if (!trains.isEmpty()) {
			trainJdbcRepository.saveAllTrains(trains);
			log.info("{}개의 열차 저장 완료", trains.size());

			// 객차 생성
			trainCarService.createTrainCars(fetchTrains(trains));
		}

		// 열차 ID가 없어서 다시 조회
		return getTrainMap();
	}

	/**
	 * 이미 존재하는 열차 조회
	 */
	private Map<Integer, Train> findExistingTrains() {
		return trainRepository.findAllWithCars().stream()
			.collect(Collectors.toMap(Train::getTrainNumber, Function.identity()));
	}

	/**
	 * 열차 ID를 가져오기 위한 메서드
	 */
	private List<Train> fetchTrains(List<Train> trains) {
		return trainRepository.findByTrainNumberIn(trains.stream()
			.map(Train::getTrainNumber)
			.toList());
	}

	public Train getTrainByNumber(int trainNumber, Map<Integer, Train> trainMap) {
		Train train = trainMap.get(trainNumber);
		if (train == null) {
			throw new IllegalArgumentException("존재하지 않는 열차입니다: " + trainNumber);
		}
		return train;
	}
}
