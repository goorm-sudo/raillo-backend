package com.sudo.railo.train.application;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.sudo.railo.train.domain.Station;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleService {

	private static final String DIR = System.getProperty("user.dir") + "/files/";
	private static final String EXCLUDE_SHEET = "총괄";
	private static final String OPERATION_DATE_COLUMN = "비고";

	private final StationService stationService;

	public void parse(String path) {
		try (FileInputStream stream = new FileInputStream(DIR + path)) {
			XSSFWorkbook workbook = new XSSFWorkbook(stream);
			workbook.forEach(sheet -> {
				if (!sheet.getSheetName().contains(EXCLUDE_SHEET)) {
					// 하행
					CellAddress downTrainAddress = getActiveCell(sheet, 0);
					parseTrainSchedule(sheet, downTrainAddress);

					// 상행
					CellAddress upTrainAddress = getActiveCell(sheet, downTrainAddress.getColumn());
					parseTrainSchedule(sheet, upTrainAddress);
				}
			});
		} catch (Exception ex) {
			log.error("유효하지 않은 열차 시간표입니다.", ex);
		}
	}

	private CellAddress getActiveCell(Sheet sheet, int start) {
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
		int trainNumberIdx = address.getColumn();
		int trainTypeIdx = address.getColumn() + 1;
		int stationIdx = address.getColumn() + 2;
		List<Station> stations = parseStations(sheet.getRow(address.getRow()), stationIdx);
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
}
