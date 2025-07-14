package com.sudo.railo.booking.infra;

import java.util.List;

import com.sudo.railo.booking.application.dto.response.TicketReadResponse;

public interface TicketRepositoryCustom {
	List<TicketReadResponse> findPaidTicketResponsesByMemberId(Long memberId);
}
