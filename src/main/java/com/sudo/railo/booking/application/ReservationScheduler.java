package com.sudo.railo.booking.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationScheduler {

	private final ReservationService reservationService;

	@Scheduled(cron = "0 * * * * *") // 매 분마다 실행
	public void expireReservations() {
		try {
			reservationService.expireReservations();
		} catch (Exception e) {
			// TODO: 로깅
		}
	}
}
