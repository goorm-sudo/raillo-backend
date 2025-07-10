package com.sudo.railo.train.infrastructure.excel;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.sudo.railo.train.application.dto.excel.ScheduleStopData;
import com.sudo.railo.train.application.dto.excel.TrainData;
import com.sudo.railo.train.application.dto.excel.TrainScheduleData;
import com.sudo.railo.train.util.OperatingDayUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TrainScheduleParser extends ExcelParser {

	private static final String EXCLUDE_SHEET = "총괄";
	private static final String OPERATING_DAY_COLUMN = "비고";
	private static final int DWELL_TIME = 2; // 정차역에 머무는 시간(분)

	@Value("${train.schedule.excel.filename}")
	private String fileName;

	@Override
	protected String getFileName() {
		return fileName;
	}

	@Override
	protected List<String> getExcludeSheetNames() {
		return List.of(EXCLUDE_SHEET);
	}

	public CellAddress getFirstCellAddress(Sheet sheet, int start) {
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

	/**
	 * 역 이름 파싱
	 */
	public List<String> parseStationNames(Sheet sheet, CellAddress address) {
		Row row = sheet.getRow(address.getRow());
		int stationIdx = getStationIdx(address);

		List<String> stationNames = new ArrayList<>();

		// 역 이름 인덱스부터 마지막까지 파싱한다.
		for (int i = stationIdx; i < row.getLastCellNum(); i++) {
			String stationName = row.getCell(i).getStringCellValue();

			// 역 이름이 아니라면 파싱을 멈춘다.
			if (stationName.contains(OPERATING_DAY_COLUMN)) {
				break;
			}
			stationNames.add(stationName);
		}
		return stationNames;
	}

	/**
	 * 운행 스케줄 파싱
	 */
	public List<TrainScheduleData> parseTrainSchedule(Sheet sheet, CellAddress address) {
		String sheetName = sheet.getSheetName();
		int stationIdx = getStationIdx(address);
		List<String> stationNames = parseStationNames(sheet, address);
		int operatingDayIdx = getOperatingDayIdx(stationIdx, stationNames.size());

		List<TrainScheduleData> trainScheduleData = new ArrayList<>();

		int rowNum = address.getRow() + 3;
		while (rowNum <= sheet.getLastRowNum()) {
			try {
				Row row = sheet.getRow(rowNum++);

				// 열, 행이 비어있다면 파싱하지 않는다.
				if (isEmpty(row, stationIdx)) {
					break;
				}

				// 열차 파싱
				TrainData trainData = extractTrainData(address, row);

				// 정차역 파싱
				List<ScheduleStopData> scheduleStopData = parseScheduleStop(row, stationIdx, stationNames);

				// 운행일 파싱
				String operatingDay = row.getCell(operatingDayIdx).getStringCellValue();
				int bitMask = OperatingDayUtil.toBitMask(operatingDay);

				// 스케줄 이름 (KTX 001 경부선, KTX-산천 075 경부선)
				String scheduleName = String.format("%s %03d %s", trainData.getTrainName(),
					trainData.getTrainNumber(), sheetName);

				// 스케줄 추가
				trainScheduleData.add(TrainScheduleData.of(scheduleName, bitMask, scheduleStopData, trainData));

			} catch (Exception ex) {
				// 스케줄 파싱에 실패해도 계속 진행
				log.warn("운행 스케줄 파싱에 실패했습니다. rowNum={}, sheetName={}", rowNum, sheet.getSheetName(), ex);
			}
		}
		return trainScheduleData;
	}

	/**
	 * 열차 정보 추출
	 */
	private TrainData extractTrainData(CellAddress address, Row row) {
		int trainNumberIdx = getTrainNumberIdx(address);
		int trainNameIdx = getTrainNameIdx(address);

		int trainNumber = (int)row.getCell(trainNumberIdx).getNumericCellValue();
		String trainName = row.getCell(trainNameIdx).getStringCellValue().replaceAll("_", "-");
		return TrainData.of(trainNumber, trainName);
	}

	/**
	 * 정차역 파싱 로직
	 */
	private List<ScheduleStopData> parseScheduleStop(Row row, int start, List<String> stationNames) {
		List<ScheduleStopData> scheduleStopData = new ArrayList<>();

		int stopOrder = 0;
		for (int i = 0; i < stationNames.size(); i++) {
			Cell cell = row.getCell(start + i);
			LocalTime departureTime = LocalTime.from(cell.getLocalDateTimeCellValue());

			// 출발 시간이 없으면 무시
			if (departureTime.equals(LocalTime.MIDNIGHT)) {
				continue;
			}

			// 도착 시간 계산
			LocalTime arrivalTime = departureTime.minusMinutes(DWELL_TIME);

			// 정차역 추가
			scheduleStopData.add(ScheduleStopData.of(stopOrder, arrivalTime, departureTime, stationNames.get(i)));
			stopOrder++;
		}

		// 첫 번째 정차역은 도착 시간이 없다.
		scheduleStopData.set(0, ScheduleStopData.first(scheduleStopData.get(0)));

		// 마지막 정차역은 출발 시간이 없다.
		int lastIndex = scheduleStopData.size() - 1;
		scheduleStopData.set(lastIndex, ScheduleStopData.last(scheduleStopData.get(lastIndex)));

		return scheduleStopData;
	}

	/**
	 * 열차 번호 인덱스
	 */
	private int getTrainNumberIdx(CellAddress address) {
		return address.getColumn();
	}

	/**
	 * 열차 이름 인덱스
	 */
	private int getTrainNameIdx(CellAddress address) {
		return address.getColumn() + 1;
	}

	/**
	 * 역 이름 인덱스
	 */
	private int getStationIdx(CellAddress address) {
		return address.getColumn() + 2;
	}

	/**
	 * 운행일 인덱스
	 */
	private int getOperatingDayIdx(int stationIdx, int stationSize) {
		return stationIdx + stationSize;
	}
}
