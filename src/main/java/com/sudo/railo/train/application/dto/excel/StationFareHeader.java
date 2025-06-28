package com.sudo.railo.train.application.dto.excel;

import org.apache.poi.ss.util.CellAddress;

public record StationFareHeader(
	int startRow,
	CellAddress section,
	CellAddress standard,
	// TODO: CellAddress superiorClass, 우등실 필요 시 구현 예정
	CellAddress firstClass
) {
}
