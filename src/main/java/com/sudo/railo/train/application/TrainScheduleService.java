package com.sudo.railo.train.application;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.sudo.railo.train.application.dto.ScheduleStopDto;
import com.sudo.railo.train.application.dto.TrainDto;
import com.sudo.railo.train.application.dto.TrainScheduleDto;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.util.ExcelUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleService {

	private static final String DIR = System.getProperty("user.dir") + "/files/";
	private static final String EXCLUDE_SHEET = "총괄";
	private static final String OPERATION_DATE_COLUMN = "비고";
	private static final String OPERATION_DATE_EVERY_DAY = "매일";
	private static final int DWELL_TIME = 2; // 정차역에 머무는 시간(분)

	private final StationService stationService;

	public void parse(String path) {
		try (FileInputStream stream = new FileInputStream(DIR + path)) {
			XSSFWorkbook workbook = new XSSFWorkbook(stream);
			workbook.forEach(sheet -> {
				if (!sheet.getSheetName().contains(EXCLUDE_SHEET)) {
					// 하행
					CellAddress downTrainAddress = getFirstCellAddress(sheet, 0);
					parseTrainSchedule(sheet, downTrainAddress);

					// 상행
					CellAddress upTrainAddress = getFirstCellAddress(sheet, downTrainAddress.getColumn() + 1);
					parseTrainSchedule(sheet, upTrainAddress);
				}
			});
		} catch (Exception ex) {
			log.error("유효하지 않은 열차 시간표입니다.", ex);
		}
	}

	private CellAddress getFirstCellAddress(Sheet sheet, int start) {
		for (int r = 0; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (ObjectUtils.isEmpty(row)) {
				continue;
			}

			for (int c = start; c < row.getLastCellNum(); c++) {
				Cell cell = row.getCell(c);
				if (!ObjectUtils.isEmpty(cell) && StringUtils.hasText(cell.toString())) {
					CellAddress address = cell.getAddress();
					return new CellAddress(address.getRow() + 1, address.getColumn());
				}
			}
		}
		throw new IllegalStateException("열차 시간표의 시작 지점을 찾을 수 없습니다.");
	}

	private void parseTrainSchedule(Sheet sheet, CellAddress address) {
		String sheetName = sheet.getSheetName();
		int trainNumberIdx = address.getColumn();
		int trainNameIdx = address.getColumn() + 1;
		int stationIdx = address.getColumn() + 2;
		List<Station> stations = parseStations(sheet.getRow(address.getRow()), stationIdx);

		int operationDateIdx = stationIdx + stations.size();
		LocalDate now = LocalDate.now();
		String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

		List<TrainScheduleDto> trainSchedulesDto = new ArrayList<>();

		int rowNum = address.getRow() + 2;
		while (rowNum++ <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(rowNum);

			// `row`가 `null`이거나, `cell`이 `null`이라면 파싱하지 않는다.
			if (ExcelUtil.isEmpty(row, trainNumberIdx)) {
				break;
			}

			// 운행일이 `매일`이 아니면서, `dayOfWeek`가 포함되지 않는다면 파싱하지 않는다.
			String operationDate = row.getCell(operationDateIdx).getStringCellValue();
			if (!operationDate.equals(OPERATION_DATE_EVERY_DAY) && !operationDate.contains(dayOfWeek)) {
				continue;
			}

			int trainNumber = (int)row.getCell(trainNumberIdx).getNumericCellValue();
			String trainName = row.getCell(trainNameIdx).getStringCellValue().replaceAll("_", "-");
			TrainDto trainDto = TrainDto.of(trainNumber, trainName);

			List<ScheduleStopDto> scheduleStopsDto = parseScheduleStop(row, stationIdx, stations);
			String scheduleName = String.format("%s-%03d %s", trainName, trainNumber, sheetName);
			trainSchedulesDto.add(TrainScheduleDto.of(scheduleName, now, scheduleStopsDto, trainDto));
		}

		// TODO: 열차 조회 및 데이터베이스에 저장
		// TODO: 열차 스케줄 저장
	}

	private List<Station> parseStations(Row row, int stationIdx) {
		List<String> stationNames = new ArrayList<>();

		// `stationIdx`위치부터 `비고`를 찾기 전까지 역 이름을 파싱한다.
		for (int i = stationIdx; i < row.getLastCellNum(); i++) {
			String stationName = row.getCell(i).getStringCellValue();
			if (stationName.contains(OPERATION_DATE_COLUMN)) {
				break;
			}
			stationNames.add(stationName);
		}

		// `station`을 조회하고, 새로운 역이 있다면 데이터베이스에 저장한다.
		List<Station> stations = stationService.getStations(stationNames);
		if (stations.size() != stationNames.size()) {
			List<Station> newStations = stationService.saveStationsIfNotExists(stationNames);
			stations.addAll(newStations);
		}
		return stations;
	}

	private List<ScheduleStopDto> parseScheduleStop(Row row, int start, List<Station> stations) {
		List<ScheduleStopDto> scheduleStopsDto = new ArrayList<>();

		int stopOrder = 0;
		for (int i = 0; i < stations.size(); i++) {
			Cell cell = row.getCell(start + i);
			LocalTime departureTime = LocalTime.from(cell.getLocalDateTimeCellValue());
			if (departureTime.equals(LocalTime.MIDNIGHT)) {
				continue;
			}

			LocalTime arrivalTime = departureTime.minusMinutes(DWELL_TIME);
			Station station = stations.get(i);
			scheduleStopsDto.add(ScheduleStopDto.of(stopOrder, arrivalTime, departureTime, station));
			stopOrder++;
		}

		// 첫 번째 정차역은 도착 시간이 `null`이다.
		scheduleStopsDto.set(0, ScheduleStopDto.first(scheduleStopsDto.get(0)));

		// 마지막 정차역은 출발 시간이 `null`이다.
		int lastIndex = scheduleStopsDto.size() - 1;
		scheduleStopsDto.set(lastIndex, ScheduleStopDto.last(scheduleStopsDto.get(lastIndex)));

		return scheduleStopsDto;
	}
}
