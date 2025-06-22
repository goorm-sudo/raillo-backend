package com.sudo.railo.train.application;

import java.io.FileInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TrainScheduleService {

	private static final String DIR = System.getProperty("user.dir") + "/files/";
	private static final String EXCLUDE_SHEET = "총괄";

	public void parse(String path) {
		try (FileInputStream stream = new FileInputStream(DIR + path)) {
			XSSFWorkbook workbook = new XSSFWorkbook(stream);
			workbook.forEach(sheet -> {
				if (!sheet.getSheetName().contains(EXCLUDE_SHEET)) {
					// 하행
					CellAddress downTrainAddress = getActiveCell(sheet, 0);

					// 상행
					CellAddress upTrainAddress = getActiveCell(sheet, downTrainAddress.getColumn());
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
}
