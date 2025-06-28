package com.sudo.railo.train.application;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.excel.StationFareHeader;
import com.sudo.railo.train.infrastructure.StationFareRepository;
import com.sudo.railo.train.infrastructure.excel.StationFareParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationFareCreator {

	private final StationFareParser parser;
	private final StationService stationService;
	private final StationFareRepository stationFareRepository;

	@Transactional
	public void createStationFare() {
		log.info("운임표 생성 시작");

		List<Sheet> sheets = parser.getSheets();
		sheets.forEach(sheet -> {
			StationFareHeader header = parser.getHeader(sheet);
		});

		log.info("운임표 생성 완료");
	}
}
