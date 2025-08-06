package com.sudo.railo.booking.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.application.dto.request.CartReservationCreateRequest;
import com.sudo.railo.booking.application.dto.response.ReservationDetail;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.CartReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.helper.ReservationTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.domain.Train;

@ServiceTest
class CartReservationServiceTest {

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private ReservationTestHelper reservationTestHelper;

	@Autowired
	private CartReservationService cartReservationService;

	@Autowired
	private CartReservationRepository cartReservationRepository;

	@Autowired
	private MemberRepository memberRepository;

	private String memberNo;
	private Reservation reservation;

	@BeforeEach
	void setUp() {
		Member member = MemberFixture.createStandardMember();
		memberNo = member.getMemberDetail().getMemberNo();
		memberRepository.save(member);

		Train train = trainTestHelper.createKTX();
		TrainScheduleWithStopStations scheduleWithStops = trainScheduleTestHelper.createSchedule(train);
		reservation = reservationTestHelper.createReservation(member, scheduleWithStops);
	}

	@Test
	@DisplayName("장바구니에 예약을 등록하는데 성공한다")
	void createCartReservation_success() {
		// given
		CartReservationCreateRequest request = new CartReservationCreateRequest(reservation.getId());

		// when
		cartReservationService.createCartReservation(memberNo, request);

		// then
		boolean exists = cartReservationRepository.existsByReservation(reservation);
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("자신의 예약이 아니라면 예외가 발생한다")
	void createCartReservation_accessDenied_throwsException() {
		// given
		Member otherMember = MemberFixture.createOtherMember();
		String otherMemberNo = otherMember.getMemberDetail().getMemberNo();
		memberRepository.save(otherMember);

		CartReservationCreateRequest request = new CartReservationCreateRequest(reservation.getId());

		// when & then
		assertThatThrownBy(() -> cartReservationService.createCartReservation(otherMemberNo, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(BookingError.RESERVATION_ACCESS_DENIED.getMessage());
	}

	@Test
	@DisplayName("장바구니에 이미 등록된 예약이라면 예외가 발생한다")
	void createCartReservation_duplicate_throwsException() {
		// given
		CartReservationCreateRequest request = new CartReservationCreateRequest(reservation.getId());

		// when & then
		assertThatThrownBy(() -> {
			cartReservationService.createCartReservation(memberNo, request);
			cartReservationService.createCartReservation(memberNo, request);
		})
			.isInstanceOf(BusinessException.class)
			.hasMessage(BookingError.RESERVATION_ALREADY_RESERVED.getMessage());
	}

	@Test
	@DisplayName("장바구니 조회에 성공한다")
	void getCartReservations_success() {
		// given
		CartReservationCreateRequest request = new CartReservationCreateRequest(reservation.getId());
		cartReservationService.createCartReservation(memberNo, request);

		// when
		List<ReservationDetail> cart = cartReservationService.getCartReservations(memberNo);

		// then
		assertThat(cart).hasSize(1);

		ReservationDetail detail = cart.get(0);
		assertThat(detail.reservationId()).isEqualTo(reservation.getId());
		assertThat(detail.seats()).isNotEmpty();
	}

	@Test
	@DisplayName("장바구니가 비어있다면 빈 응답을 반환한다")
	void getCartReservations_empty_success() {
		// when
		List<ReservationDetail> cart = cartReservationService.getCartReservations(memberNo);

		// then
		assertThat(cart).isEmpty();
	}
}
