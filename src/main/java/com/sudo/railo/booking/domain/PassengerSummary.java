package com.sudo.railo.booking.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "승객 유형과 인원수")
public class PassengerSummary {

	@Schema(description = "승객 유형 (ERD 참고)", example = "ADULT")
	private PassengerType passengerType;

	@Schema(description = "승객 수", example = "2")
	private int count;
}
