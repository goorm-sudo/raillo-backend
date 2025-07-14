package com.sudo.railo.booking.presentation;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.booking.application.CartReservationService;
import com.sudo.railo.booking.application.dto.request.CartReservationCreateRequest;
import com.sudo.railo.booking.application.dto.response.ReservationDetail;
import com.sudo.railo.booking.success.CartReservationSuccess;
import com.sudo.railo.global.success.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cart/reservations")
@RequiredArgsConstructor
@Tag(name = "CartReservations", description = "장바구니에 예약 등록, 조회 API")
public class CartReservationController {

	private final CartReservationService cartReservationService;

	@PostMapping
	@Operation(summary = "예약 등록", description = "장바구니에 예약을 등록합니다.")
	public SuccessResponse<?> createCartReservation(
		@Valid @RequestBody CartReservationCreateRequest request,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		String memberNo = userDetails.getUsername();

		cartReservationService.createCartReservation(memberNo, request);
		return SuccessResponse.of(CartReservationSuccess.CART_RESERVATION_CREATE_SUCCESS);
	}

	@GetMapping
	@Operation(summary = "장바구니 조회", description = "장바구니에 등록된 예약을 조회합니다.")
	public SuccessResponse<List<ReservationDetail>> getCartReservations(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		String memberNo = userDetails.getUsername();

		List<ReservationDetail> response = cartReservationService.getCartReservations(memberNo);
		return SuccessResponse.of(CartReservationSuccess.CART_RESERVATION_LIST_SUCCESS, response);
	}
}
