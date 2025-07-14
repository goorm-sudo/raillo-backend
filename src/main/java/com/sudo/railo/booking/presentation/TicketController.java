package com.sudo.railo.booking.presentation;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.booking.application.TicketService;
import com.sudo.railo.booking.application.dto.response.TicketReadResponse;
import com.sudo.railo.booking.docs.TicketControllerDocs;
import com.sudo.railo.booking.success.TicketSuccess;
import com.sudo.railo.global.success.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/booking/ticket")
@RequiredArgsConstructor
public class TicketController implements TicketControllerDocs {

	private final TicketService ticketService;

	@GetMapping
	public SuccessResponse<List<TicketReadResponse>> getMyTickets(@AuthenticationPrincipal UserDetails userDetails) {
		List<TicketReadResponse> tickets = ticketService.getMyTickets(userDetails);
		return SuccessResponse.of(TicketSuccess.TICKET_LIST_GET_SUCCESS, tickets);
	}
}
