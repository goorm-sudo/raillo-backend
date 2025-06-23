package com.sudo.railo.train.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatType;
import com.sudo.railo.train.domain.type.TrainType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "train-template")
public class TrainTemplateProperties {

	private final Map<TrainType, TrainTemplate> templates;

	public record TrainTemplate(Map<CarType, SeatLayout> layouts, List<CarConfig> cars) {
	}

	public record SeatLayout(String seatArrangement, List<SeatConfig> columns) {
	}

	public record SeatConfig(String name, SeatType seatType) {
	}

	public record CarConfig(CarType carType, int row) {
	}
}
