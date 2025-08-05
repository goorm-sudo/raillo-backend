package com.sudo.railo.support.helper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.ScheduleStopTemplate;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.StationFare;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.TrainScheduleTemplate;
import com.sudo.railo.train.infrastructure.ScheduleStopRepository;
import com.sudo.railo.train.infrastructure.StationFareRepository;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class TrainScheduleTestHelper {

	public static final int EVERYDAY = 127;

	private final TrainScheduleRepository trainScheduleRepository;
	private final ScheduleStopRepository scheduleStopRepository;
	private final StationRepository stationRepository;
	private final StationFareRepository stationFareRepository;
	private final TrainScheduleTemplateRepository trainScheduleTemplateRepository;

	/**
	 * 기본 스케줄 생성 메서드
	 * 서울 -> 부산 (출발: 5:00 -> 도착: 8:00)
	 * standardFare: 50,000원, firstClassFare: 100,000원
	 */
	public TrainScheduleWithStopStations createSchedule(Train train) {
		createOrUpdateStationFare("서울", "부산", 50000, 100000);
		return createCustomSchedule()
			.scheduleName("KTX 001 경부선")
			.operationDate(LocalDate.now())
			.departureTime(LocalTime.of(5, 0))
			.arrivalTime(LocalTime.of(8, 0))
			.train(train)
			.addStop("서울", null, LocalTime.of(5, 0))
			.addStop("부산", LocalTime.of(8, 0), null)
			.build();
	}

	/**
	 * 커스텀 스케줄 빌더
	 */
	public TrainScheduleBuilder createCustomSchedule() {
		return new TrainScheduleBuilder();
	}

	/**
	 * 역 + 구간요금 생성 (없으면 생성, 있으면 갱신)
	 */
	public void createOrUpdateStationFare(String from, String to, int standardFare, int firstClassFare) {
		Station departure = getOrCreateStation(from);
		Station arrival = getOrCreateStation(to);

		StationFare fare = StationFare.create(departure, arrival, standardFare, firstClassFare);
		stationFareRepository.save(fare);
	}

	/**
	 * 특정 역의 정차 정보 조회
	 */
	public ScheduleStop getScheduleStopByStationName(TrainScheduleWithStopStations schedule, String stationName) {
		return schedule.scheduleStops().stream()
			.filter(s -> s.getStation().getStationName().equals(stationName))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("정차역을 찾을 수 없습니다: " + stationName));
	}

	/**
	 * 역 생성 또는 조회
	 */
	public Station getOrCreateStation(String stationName) {
		return stationRepository.findByStationName(stationName)
			.orElseGet(() -> stationRepository.save(Station.create(stationName)));
	}

	/**
	 * TrainSchedule 생성용 Builder
	 */
	public class TrainScheduleBuilder {
		private final List<StopInfo> stops = new ArrayList<>();
		private String scheduleName;
		private LocalDate operationDate;
		private LocalTime departureTime;
		private LocalTime arrivalTime;
		private Train train;
		private int operatingDays = EVERYDAY;

		public TrainScheduleBuilder scheduleName(String name) {
			this.scheduleName = name;
			return this;
		}

		public TrainScheduleBuilder operationDate(LocalDate date) {
			this.operationDate = date;
			return this;
		}

		public TrainScheduleBuilder departureTime(LocalTime time) {
			this.departureTime = time;
			return this;
		}

		public TrainScheduleBuilder arrivalTime(LocalTime time) {
			this.arrivalTime = time;
			return this;
		}

		public TrainScheduleBuilder train(Train train) {
			this.train = train;
			return this;
		}

		public TrainScheduleBuilder operatingDays(int days) {
			this.operatingDays = days;
			return this;
		}

		public TrainScheduleBuilder addStop(String stationName, LocalTime arrival, LocalTime departure) {
			stops.add(new StopInfo(stationName, arrival, departure, stops.size()));
			return this;
		}

		@Transactional
		public TrainScheduleWithStopStations build() {
			Map<String, Station> stationMap = resolveStations();
			List<ScheduleStopTemplate> stopTemplates = buildStopTemplates(stationMap);
			TrainScheduleTemplate template = saveTemplate(stationMap, stopTemplates);
			TrainSchedule schedule = saveSchedule(template);
			List<ScheduleStop> savedStops = saveScheduleStops(template, schedule);
			return new TrainScheduleWithStopStations(schedule, savedStops);
		}

		private Map<String, Station> resolveStations() {
			Map<String, Station> map = new HashMap<>();
			stops.forEach(stop -> map.putIfAbsent(stop.stationName(), getOrCreateStation(stop.stationName())));
			return map;
		}

		private List<ScheduleStopTemplate> buildStopTemplates(Map<String, Station> stationMap) {
			return stops.stream()
				.map(s -> ScheduleStopTemplate.create(
					s.stopOrder(),
					s.arrivalTime(),
					s.departureTime(),
					stationMap.get(s.stationName())
				)).toList();
		}

		private TrainScheduleTemplate saveTemplate(Map<String, Station> stationMap,
			List<ScheduleStopTemplate> stopTemplates) {
			Station departure = stationMap.get(stops.get(0).stationName());
			Station arrival = stationMap.get(stops.get(stops.size() - 1).stationName());

			TrainScheduleTemplate template = TrainScheduleTemplate.create(
				scheduleName,
				operatingDays,
				departureTime,
				arrivalTime,
				train,
				departure,
				arrival,
				stopTemplates
			);
			return trainScheduleTemplateRepository.save(template);
		}

		private TrainSchedule saveSchedule(TrainScheduleTemplate template) {
			return trainScheduleRepository.save(TrainSchedule.create(operationDate, template));
		}

		private List<ScheduleStop> saveScheduleStops(TrainScheduleTemplate template, TrainSchedule schedule) {
			List<ScheduleStop> stops = template.getScheduleStops().stream()
				.map(t -> ScheduleStop.create(t, schedule))
				.toList();
			return scheduleStopRepository.saveAll(stops);
		}
	}

	/**
	 * 생성 결과 객체 (스케줄 + 정차역들)
	 */
	public record TrainScheduleWithStopStations(TrainSchedule trainSchedule, List<ScheduleStop> scheduleStops) {
	}

	/**
	 * 정차역 임시 정보 (Builder 내부용)
	 */
	private record StopInfo(String stationName, LocalTime arrivalTime, LocalTime departureTime, int stopOrder) {
	}
}
