package com.sudo.railo.booking.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.booking.application.ReservationApplicationService;
import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.application.dto.response.ReservationCreateResponse;
import com.sudo.railo.booking.success.ReservationSuccess;
import com.sudo.railo.global.success.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/booking/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationApplicationService reservationApplicationService;

	/***
	 * 예약을 생성하는 메서드
	 * @param request 예약 생성 요청 DTO
	 * @return 예약 생성 성공 응답
	 */
	@PostMapping
	public SuccessResponse<ReservationCreateResponse> createReservation(
		@RequestBody ReservationCreateRequest request,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		ReservationCreateResponse response = reservationApplicationService.createReservation(request, userDetails);
		return SuccessResponse.of(ReservationSuccess.RESERVATION_CREATE_SUCCESS, response);
	}

	/***
	 * 예약을 삭제하는 메서드
	 * @param request 예약 삭제 요청 DTO
	 * @return 예약 삭제 성공 응답
	 */
	@DeleteMapping
	public SuccessResponse<?> deleteReservation(@RequestBody ReservationDeleteRequest request) {
		reservationApplicationService.deleteReservation(request);
		return SuccessResponse.of(ReservationSuccess.RESERVATION_DELETE_SUCCESS);
	}
}
