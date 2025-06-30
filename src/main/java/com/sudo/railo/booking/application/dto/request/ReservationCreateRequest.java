package com.sudo.railo.booking.application.dto.request;

import com.sudo.railo.booking.domain.PassengerSummary;
import com.sudo.railo.booking.domain.TripType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationCreateRequest {

	@NotNull(message = "열차 스케줄 ID는 필수입니다")
	private Long trainScheduleId;

	private Long seatId;

	// @NotNull(message = "회원 ID는 필수입니다")
	// private Long memberId;

	@NotNull(message = "출발역 ID는 필수입니다")
	private Long departureStationId;

	@NotNull(message = "도착역 ID는 필수입니다")
	private Long arrivalStationId;

	@NotNull(message = "승객 정보는 필수입니다")
	private PassengerSummary passengerSummary;

	@NotNull(message = "여행 타입은 필수입니다")
	private TripType tripType;
}
