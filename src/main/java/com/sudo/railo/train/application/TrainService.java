package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.TrainDto;
import com.sudo.railo.train.config.TrainTemplateProperties;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.infrastructure.persistence.TrainRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainService {

	private final TrainTemplateProperties properties;
	private final TrainRepository trainRepository;

	@Transactional
	public Map<Integer, Train> findOrCreateTrains(List<TrainDto> trainDtos) {
		Map<Integer, Train> trainMap = findExistingTrains(trainDtos);
		List<Train> newTrains = createTrains(trainDtos, trainMap);

		if (!newTrains.isEmpty()) {
			trainRepository.saveAll(newTrains);
			newTrains.forEach(train -> trainMap.put(train.getTrainNumber(), train));
		}
		return trainMap;
	}

	private Map<Integer, Train> findExistingTrains(List<TrainDto> trainDtos) {
		List<Integer> trainNumbers = trainDtos.stream()
			.map(TrainDto::getTrainNumber)
			.toList();
		return trainRepository.findByTrainNumberIn(trainNumbers).stream()
			.collect(Collectors.toMap(Train::getTrainNumber, train -> train));
	}

	private List<Train> createTrains(List<TrainDto> trainDtos, Map<Integer, Train> existing) {
		return trainDtos.stream()
			.filter(dto -> !existing.containsKey(dto.getTrainNumber()))
			.map(dto -> Train.create(
				dto.getTrainNumber(),
				dto.getTrainType(),
				dto.getTrainName(),
				properties.getLayouts(),
				properties.getTemplates().get(dto.getTrainType())
			)).toList();
	}
}
