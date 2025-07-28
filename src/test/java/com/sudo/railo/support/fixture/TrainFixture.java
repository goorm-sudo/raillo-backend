package com.sudo.railo.support.fixture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sudo.railo.train.config.TrainTemplateProperties.CarSpec;
import com.sudo.railo.train.config.TrainTemplateProperties.SeatColumn;
import com.sudo.railo.train.config.TrainTemplateProperties.SeatLayout;
import com.sudo.railo.train.config.TrainTemplateProperties.TrainTemplate;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatType;
import com.sudo.railo.train.domain.type.TrainType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainFixture {

	KTX(1, TrainType.KTX, "KTX", 18,
		List.of(
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.FIRST_CLASS, 9),
			new CarSpec(CarType.FIRST_CLASS, 12),
			new CarSpec(CarType.FIRST_CLASS, 11),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 14)
		)
	),

	KTX_SANCHEON(
		2, TrainType.KTX_SANCHEON, "KTX-산천", 8,
		List.of(
			new CarSpec(CarType.FIRST_CLASS, 11),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 12),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 12),
			new CarSpec(CarType.STANDARD, 14),
			new CarSpec(CarType.STANDARD, 15)
		)
	),

	KTX_CHEONGRYONG(
		3, TrainType.KTX_CHEONGRYONG, "KTX-청룡", 8,
		List.of(
			new CarSpec(CarType.FIRST_CLASS, 12),
			new CarSpec(CarType.STANDARD, 19),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 17),
			new CarSpec(CarType.STANDARD, 19),
			new CarSpec(CarType.STANDARD, 17),
			new CarSpec(CarType.STANDARD, 19),
			new CarSpec(CarType.STANDARD, 13)
		)
	),

	KTX_EUM(
		4, TrainType.KTX_EUM, "KTX-이음", 6,
		List.of(
			new CarSpec(CarType.FIRST_CLASS, 12),
			new CarSpec(CarType.STANDARD, 20),
			new CarSpec(CarType.STANDARD, 15),
			new CarSpec(CarType.STANDARD, 19),
			new CarSpec(CarType.STANDARD, 19),
			new CarSpec(CarType.STANDARD, 13)
		)
	);

	private final int trainNumber;
	private final TrainType trainType;
	private final String trainName;
	private final int totalCars;
	private final List<CarSpec> carSpecs;

	public Train create() {
		return Train.create(trainNumber, trainType, trainName, totalCars);
	}

	public TrainTemplate createTrainTemplate() {
		return new TrainTemplate(carSpecs);
	}

	public CarSpec getCarSpecByCarNumber(int carNumber) {
		return carSpecs.get(carNumber - 1);
	}

	public static Map<CarType, SeatLayout> createSeatLayouts() {
		Map<CarType, SeatLayout> layouts = new HashMap<>();

		List<SeatColumn> standardColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE),
			new SeatColumn("C", SeatType.AISLE),
			new SeatColumn("D", SeatType.WINDOW)
		);
		layouts.put(CarType.STANDARD, new SeatLayout("2+2", standardColumns));

		List<SeatColumn> firstClassColumns = List.of(
			new SeatColumn("A", SeatType.WINDOW),
			new SeatColumn("B", SeatType.AISLE),
			new SeatColumn("C", SeatType.WINDOW)
		);
		layouts.put(CarType.FIRST_CLASS, new SeatLayout("2+1", firstClassColumns));

		return layouts;
	}

	public static SeatLayout getSeatLayoutByCarType(CarType carType) {
		return createSeatLayouts().get(carType);
	}

	public static Train KTX() {
		return KTX.create();
	}

	public static Train KTX_SANCHEON() {
		return KTX_SANCHEON.create();
	}

	public static Train KTX_CHEONGRYONG() {
		return KTX_CHEONGRYONG.create();
	}

	public static Train KTX_EUM() {
		return KTX_EUM.create();
	}
}
