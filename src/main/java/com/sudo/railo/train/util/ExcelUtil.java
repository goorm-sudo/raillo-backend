package com.sudo.railo.train.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.util.ObjectUtils;

public class ExcelUtil {

	public static boolean isEmpty(Row row, int cellNum) {
		if (ObjectUtils.isEmpty(row)) {
			return true;
		}

		Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return ObjectUtils.isEmpty(cell);
	}
}
