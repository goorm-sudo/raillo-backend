package com.sudo.railo.booking.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.booking.application.dto.request.CartReservationCreateRequest;
import com.sudo.railo.booking.application.dto.response.ReservationDetail;
import com.sudo.railo.booking.domain.CartReservation;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.CartReservationRepository;
import com.sudo.railo.booking.infra.ReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartReservationService {

	private final ReservationService reservationService;
	private final MemberRepository memberRepository;
	private final ReservationRepository reservationRepository;
	private final CartReservationRepository cartReservationRepository;

	/**
	 * 장바구니에 예약 등록
	 */
	@Transactional
	public void createCartReservation(String memberNo, CartReservationCreateRequest request) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		Reservation reservation = reservationRepository.findById(request.reservationId())
			.orElseThrow(() -> new BusinessException(BookingError.RESERVATION_NOT_FOUND));

		// 본인 예약인지 확인
		validateReservationAccess(member, reservation);

		// 장바구니에 등록된 예약인지 확인
		if (cartReservationRepository.existsByReservation(reservation)) {
			throw new BusinessException(BookingError.RESERVATION_ALREADY_RESERVED);
		}

		CartReservation cartReservation = CartReservation.create(member, reservation);
		cartReservationRepository.save(cartReservation);
	}

	/**
	 * 장바구니에 등록한 예약 조회
	 */
	@Transactional(readOnly = true)
	public List<ReservationDetail> getCartReservations(String memberNo) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		// 장바구니에 등록된 예약 ID 조회
		List<Long> reservationIds = cartReservationRepository.findReservationIdsByMember(member);

		// 예약 조회
		return reservationService.getReservationDetail(reservationIds);
	}

	private void validateReservationAccess(Member member, Reservation reservation) {
		Long reservationMemberId = reservation.getMember().getId();
		if (!reservationMemberId.equals(member.getId())) {
			log.warn("권한 없는 접근 시도: memberId={}, reservationId={}", member.getId(), reservation.getId());
			throw new BusinessException(BookingError.RESERVATION_ACCESS_DENIED);
		}
	}
}
