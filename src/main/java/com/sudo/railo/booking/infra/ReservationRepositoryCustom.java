package com.sudo.railo.booking.infra;

import java.util.List;

import com.sudo.railo.booking.application.dto.ReservationInfo;

public interface ReservationRepositoryCustom {

	List<ReservationInfo> findReservationDetail(List<Long> reservationIds);
}
