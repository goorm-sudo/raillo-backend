package com.sudo.railo.train.application.validator;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.exception.TrainErrorCode;

@Component
public class TrainSearchValidator {

	/**
	 * 열차 조회 request 비즈니스 검증 메서드
	 */
	public void validateTrainSearchRequest(TrainSearchRequest request) {
		validateRoute(request);
		validateOperationDate(request);
		validateDepartureTime(request);
	}

	private void validateRoute(TrainSearchRequest request) {
		if (request.departureStationId().equals(request.arrivalStationId())) {
			throw new BusinessException(TrainErrorCode.INVALID_ROUTE);
		}
	}

	private void validateOperationDate(TrainSearchRequest request) {
		if (request.operationDate().isAfter(LocalDate.now().plusMonths(1))) {
			throw new BusinessException(TrainErrorCode.OPERATION_DATE_TOO_FAR);
		}
	}

	private void validateDepartureTime(TrainSearchRequest request) {
		if (request.operationDate().equals(LocalDate.now())) {
			int requestHour = Integer.parseInt(request.departureHour());
			int currentHour = LocalTime.now().getHour();

			if (requestHour < currentHour) {
				throw new BusinessException(TrainErrorCode.DEPARTURE_TIME_PASSED);
			}
		}
	}
}
