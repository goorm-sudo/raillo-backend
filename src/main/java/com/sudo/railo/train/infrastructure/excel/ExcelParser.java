package com.sudo.railo.train.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.util.ObjectUtils;

abstract class ExcelParser {

	private static final String DIR = System.getProperty("user.dir") + "/files/";

	protected String getPath(String path) {
		return DIR + path;
	}

	protected boolean isEmpty(Row row, int cellNum) {
		if (ObjectUtils.isEmpty(row)) {
			return true;
		}

		Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return ObjectUtils.isEmpty(cell);
	}
}
