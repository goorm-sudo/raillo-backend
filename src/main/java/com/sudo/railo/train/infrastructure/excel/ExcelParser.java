package com.sudo.railo.train.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.util.ObjectUtils;

abstract class ExcelParser {

	private static final String FILES_DIR = System.getProperty("user.dir") + "/files/";

	protected String getFilePath(String fileName) {
		return FILES_DIR + fileName;
	}

	protected boolean isEmpty(Row row, int cellNum) {
		if (ObjectUtils.isEmpty(row)) {
			return true;
		}

		Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return ObjectUtils.isEmpty(cell);
	}
}
