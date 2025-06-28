package com.sudo.railo.train.infrastructure.excel;

import java.util.HashMap;
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

import com.sudo.railo.train.application.dto.excel.StationFareHeader;
import com.sudo.railo.train.application.dto.excel.StationFareHeaderType;

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
			last,
			headerMap.get(StationFareHeaderType.SECTION),
			headerMap.get(StationFareHeaderType.STANDARD),
			headerMap.getOrDefault(StationFareHeaderType.FIRST_CLASS,
				headerMap.get(StationFareHeaderType.SUPERIOR_CLASS))
		);
	}
}
