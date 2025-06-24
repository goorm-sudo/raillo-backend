package com.sudo.railo.train.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record StandingTypeInfo(
	@Schema(description = "입석 가능 인원", example = "25")
	int availableStanding,

	@Schema(description = "최대 입석 인원", example = "50")
	int maxStanding,

	@Schema(description = "입석 요금", example = "12690")
	int fare,

	@Schema(description = "화면 표시용 텍스트", example = "입석")
	String displayText
) {
	public static StandingTypeInfo create(int availableStanding, int maxStanding, int fare) {
		return new StandingTypeInfo(availableStanding, maxStanding, fare, "입석");
	}
}
