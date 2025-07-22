package com.sudo.railo.booking.infrastructure.ticket;

import java.util.List;

import com.sudo.railo.booking.application.dto.response.TicketReadResponse;

public interface TicketRepositoryCustom {
	List<TicketReadResponse> findPaidTicketResponsesByMemberId(Long memberId);
}
