package com.sudo.railo.train.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.TrainType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "train-template")
public class TrainTemplateProperties {

	private final Map<TrainType, TrainTemplate> templates;

	public record TrainTemplate(Map<String, SeatLayout> layouts, List<CarConfig> cars) {
	}

	public record SeatLayout(List<String> columns) {
	}

	public record CarConfig(CarType carType, int row) {
	}
}
