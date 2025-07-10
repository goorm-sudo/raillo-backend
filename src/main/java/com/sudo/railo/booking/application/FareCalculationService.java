package com.sudo.railo.booking.application;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.request.FareCalculateRequest;
import com.sudo.railo.booking.domain.PassengerType;

@Service
public class FareCalculationService {

	private static final Map<PassengerType, BigDecimal> DISCOUNT_RATES = Map.of(
		PassengerType.ADULT, BigDecimal.valueOf(1.0), // 정상가
		PassengerType.CHILD, BigDecimal.valueOf(0.6), // 10~40% 할인
		PassengerType.INFANT, BigDecimal.valueOf(0.25), // 좌석 지정 시 75% 할인, 좌석 지정 안하면 100% 할인
		PassengerType.SENIOR, BigDecimal.valueOf(0.7), // 30% 할인
		PassengerType.DISABLED_HEAVY, BigDecimal.valueOf(0.5), // 50% 할인 (보호자 1인 포함)
		PassengerType.DISABLED_LIGHT, BigDecimal.valueOf(0.7), // 30% 할인
		PassengerType.VETERAN, BigDecimal.valueOf(0.5) // 연 6회 무임, 6회 초과 시 50% 할인
	);

	/***
	 * 승객 유형별로 내야 할 금액을 계산하는 메서드
	 * @param request 승객 유형, 원래 운임을 포함하는 DTO
	 * @return 할인이 적용 된 운임
	 */
	public BigDecimal calculateFare(FareCalculateRequest request) {
		BigDecimal discountRate = DISCOUNT_RATES.get(request.passengerType());
		return request.fare().multiply(discountRate);
	}
}
