package com.sudo.railo.train.application.dto.excel;

import java.util.Objects;

public record StationFareData(
	String departureStation,
	String arrivalStation,
	int standardFare,
	int firstClassFare
) {

	// TODO: 중복 구간을 제거하는 용도로 equals와 hashCode를 오버라이드 했습니다.
	// 열차 타입에 따른 차등 운임을 적용할 때 제거해야 합니다.
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		StationFareData data = (StationFareData)obj;
		return Objects.equals(departureStation, data.departureStation) &&
			Objects.equals(arrivalStation, data.arrivalStation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(departureStation, arrivalStation);
	}
}
