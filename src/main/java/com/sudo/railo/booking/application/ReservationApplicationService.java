package com.sudo.railo.booking.application;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.application.dto.response.ReservationCreateResponse;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationApplicationService {

	private final ReservationService reservationService;
	private final SeatReservationService seatReservationService;

	@Transactional
	public ReservationCreateResponse createReservation(ReservationCreateRequest request, UserDetails userDetails) {
		Reservation reservation = reservationService.createReservation(request, userDetails);
		SeatReservation seatReservation = seatReservationService.reserveNewSeat(reservation, request);
		return new ReservationCreateResponse(reservation.getId(), seatReservation.getId());
	}

	public void deleteReservation(ReservationDeleteRequest request) {
		reservationService.deleteReservation(request);
	}
}
