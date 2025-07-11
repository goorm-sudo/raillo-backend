package com.sudo.railo.booking.docs;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;

import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.application.dto.response.ReservationCreateResponse;
import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reservations")
public interface ReservationControllerDocs {

	@Operation(method = "POST", summary = "예약 생성", description = "정보를 받아 예약을 수행합니다.", security = {
		@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "예약이 성공적으로 생성되었습니다."),
		@ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "요청한 유저, 역, 요청한 좌석을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<ReservationCreateResponse> createReservation(@RequestBody ReservationCreateRequest request,
		@AuthenticationPrincipal UserDetails userDetails);

	SuccessResponse<?> deleteReservation(@RequestBody ReservationDeleteRequest request);
}
