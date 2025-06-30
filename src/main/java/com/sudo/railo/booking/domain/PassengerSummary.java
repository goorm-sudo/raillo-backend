package com.sudo.railo.booking.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PassengerSummary {

	private int adult;
	private int child;
	private int senior;

	public int getPassengerCount() {
		return adult + child + senior;
	}
}
