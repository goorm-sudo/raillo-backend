package com.sudo.railo.booking.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.response.ReservationCreateResponse;
import com.sudo.railo.booking.domain.PassengerSummary;
import com.sudo.railo.booking.domain.PassengerType;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.infrastructure.SeatRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationApplicationService {

	private final ReservationService reservationService;
	private final TicketService ticketService;
	private final SeatReservationService seatReservationService;
	private final SeatRepository seatRepository;

	@Transactional
	public ReservationCreateResponse createReservation(ReservationCreateRequest request, UserDetails userDetails) {
		// TODO: 요청 파라미터를 여기서 모두 검증할지, 각 서비스에서 검증할지 결정 필요
		Reservation reservation = reservationService.createReservation(request, userDetails);

		// 승객 정보, 좌석 정보 정렬 (승객 정보는 PassengerType에 정의한 순서대로, 좌석 정보는 오름차순)
		List<PassengerSummary> passengers = request.passengers();
		passengers.sort(Comparator.comparingInt(ps -> ps.getPassengerType().ordinal()));
		List<Long> seatIds = request.seatIds();
		seatIds.sort(Comparator.naturalOrder());

		// 요청 승객 수와 선택한 좌석 수를 비교하여 좌석 수가 승객 수보다 많으면 오류 발생
		int passengersCnt = passengers.stream()
			.mapToInt(PassengerSummary::getCount)
			.sum();
		if (passengersCnt != seatIds.size()) {
			throw new BusinessException(BookingError.RESERVATION_CREATE_SEATS_INVALID);
		}

		// 좌석 차례대로 승객 할당
		int idx = 0;
		List<Long> seatReservationIds = new ArrayList<>();
		for (PassengerSummary passenger : passengers) {
			PassengerType passengerType = passenger.getPassengerType();
			int passengerCnt = passenger.getCount();
			for (int i = 0; i < passengerCnt && idx < seatIds.size(); i++, idx++) {
				ticketService.createTicket(reservation, passengerType);
				Seat seat = seatRepository.findById(seatIds.get(idx))
					.orElseThrow(() -> new BusinessException((BookingError.SEAT_NOT_FOUND)));
				SeatReservation seatReservation = seatReservationService.reserveNewSeat(reservation, seat,
					passengerType);
				seatReservationIds.add(seatReservation.getId());
			}
		}
		return new ReservationCreateResponse(reservation.getId(), seatReservationIds);
	}
}
