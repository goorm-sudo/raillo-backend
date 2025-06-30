package com.sudo.railo.booking.presentation;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.booking.application.FareCalculationService;
import com.sudo.railo.booking.application.dto.request.FareCalculateRequest;
import com.sudo.railo.booking.success.FareSuccess;
import com.sudo.railo.global.success.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {

	private final FareCalculationService fareCalculationService;

	/***
	 * 승객 유형과 운임을 입력받아 할인된 운임을 계산하는 메서드
	 * @param request 승객 유형과 운임이 담긴 DTO
	 * @return 할인된 운임 가격
	 */
	@PostMapping("/fare")
	public SuccessResponse<BigDecimal> calculateFare(@RequestBody FareCalculateRequest request) {
		BigDecimal newFare = fareCalculationService.calculateFare(request);
		return SuccessResponse.of(FareSuccess.FARE_CALCULATE_SUCCESS, newFare);
	}
}
