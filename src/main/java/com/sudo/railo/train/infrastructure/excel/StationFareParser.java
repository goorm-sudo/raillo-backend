package com.sudo.railo.train.infrastructure.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.sudo.railo.train.application.dto.excel.StationFareData;
import com.sudo.railo.train.application.dto.excel.StationFareHeader;
import com.sudo.railo.train.application.dto.excel.StationFareHeaderType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StationFareParser extends ExcelParser {

	@Value("${train.station-fare.excel.filename}")
	private String fileName;

	@Override
	protected String getFileName() {
		return fileName;
	}

	public StationFareHeader getHeader(Sheet sheet) {
		Map<StationFareHeaderType, CellAddress> headerMap = new HashMap<>();

		int last = sheet.getMergedRegions().stream()
			.mapToInt(CellRangeAddress::getLastRow)
			.max()
			.orElse(sheet.getLastRowNum());

		for (int r = 0; r <= last; r++) {
			Row row = sheet.getRow(r);
			if (ObjectUtils.isEmpty(row)) {
				continue;
			}

			for (int c = 0; c < row.getLastCellNum(); c++) {
				Cell cell = row.getCell(c);
				if (ObjectUtils.isEmpty(cell) || !StringUtils.hasText(cell.toString())) {
					continue;
				}

				String cellValue = cell.toString().replaceAll(" ", "");
				StationFareHeaderType type = StationFareHeaderType.from(cellValue);
				if (type != null) {
					headerMap.putIfAbsent(type, cell.getAddress());
				}
			}
		}

		return new StationFareHeader(
			last + 1,
			headerMap.get(StationFareHeaderType.SECTION),
			headerMap.get(StationFareHeaderType.STANDARD),
			headerMap.getOrDefault(StationFareHeaderType.FIRST_CLASS,
				headerMap.get(StationFareHeaderType.SUPERIOR_CLASS))
		);
	}

	public List<StationFareData> getStationFareData(Sheet sheet, StationFareHeader header) {
		int departureStationIdx = header.section().getColumn();
		int arrivalStationIdx = header.section().getColumn() + 1;
		int standardIdx = header.standard().getColumn();
		int firstClassIdx = header.firstClass().getColumn() + 2;

		List<StationFareData> stationFareData = new ArrayList<>();

		for (int rowNum = header.startRow(); rowNum <= sheet.getLastRowNum(); rowNum++) {
			try {
				Row row = sheet.getRow(rowNum);

				// `row`가 `null`이거나, `cell`이 `null`이라면 파싱하지 않는다.
				if (isEmpty(row, departureStationIdx)) {
					break;
				}

				String departureStation = row.getCell(departureStationIdx).getStringCellValue();
				String arrivalStation = row.getCell(arrivalStationIdx).getStringCellValue();
				int standardFare = (int)row.getCell(standardIdx).getNumericCellValue();
				int firstClassFare = (int)row.getCell(firstClassIdx).getNumericCellValue();

				stationFareData.add(new StationFareData(
					departureStation,
					arrivalStation,
					standardFare,
					firstClassFare
				));
			} catch (Exception ex) {
				log.error("운임표 파싱 중 예외가 발생했습니다", ex);
			}
		}
		return stationFareData;
	}
}
