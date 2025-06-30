package com.sudo.railo.train.infrastructure.excel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.ObjectUtils;

abstract class ExcelParser {

	private static final String FILES_DIR = System.getProperty("user.dir") + "/files/";

	protected abstract String getFileName();

	protected List<String> getExcludeSheetNames() {
		return List.of();
	}

	public List<Sheet> getSheets() {
		List<Sheet> sheets = new ArrayList<>();
		try (FileInputStream stream = new FileInputStream(FILES_DIR + getFileName())) {
			Workbook workbook = WorkbookFactory.create(stream);
			for (Sheet sheet : workbook) {
				boolean isExcluded = getExcludeSheetNames().stream()
					.anyMatch(excludeName -> sheet.getSheetName().contains(excludeName));

				if (!isExcluded) {
					sheets.add(sheet);
				}
			}
		} catch (FileNotFoundException ex) {
			throw new IllegalStateException("파일을 찾을 수 없습니다: " + getFileName(), ex);
		} catch (IOException ex) {
			throw new IllegalStateException("파일을 읽을 수 없습니다." + getFileName(), ex);
		}
		return sheets;
	}

	protected boolean isEmpty(Row row, int cellNum) {
		if (ObjectUtils.isEmpty(row)) {
			return true;
		}

		Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return ObjectUtils.isEmpty(cell);
	}
}
