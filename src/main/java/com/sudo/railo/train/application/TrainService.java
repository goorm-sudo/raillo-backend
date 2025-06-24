package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.TrainData;
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
	public Map<Integer, Train> findOrCreateTrains(List<TrainData> trainData) {
		Map<Integer, Train> trainMap = findExistingTrains(trainData);
		List<Train> newTrains = createTrains(trainData, trainMap);

		if (!newTrains.isEmpty()) {
			trainRepository.saveAll(newTrains);
			newTrains.forEach(train -> trainMap.put(train.getTrainNumber(), train));
		}
		return trainMap;
	}

	private Map<Integer, Train> findExistingTrains(List<TrainData> trainData) {
		List<Integer> trainNumbers = trainData.stream()
			.map(TrainData::getTrainNumber)
			.toList();
		return trainRepository.findByTrainNumberIn(trainNumbers).stream()
			.collect(Collectors.toMap(Train::getTrainNumber, train -> train));
	}

	private List<Train> createTrains(List<TrainData> trainData, Map<Integer, Train> existing) {
		return trainData.stream()
			.filter(data -> !existing.containsKey(data.getTrainNumber()))
			.map(data -> Train.create(
				data.getTrainNumber(),
				data.getTrainType(),
				data.getTrainName(),
				properties.getLayouts(),
				properties.getTemplates().get(data.getTrainType())
			)).toList();
	}
}
