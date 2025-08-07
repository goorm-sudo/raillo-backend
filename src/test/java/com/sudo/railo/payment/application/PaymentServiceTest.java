package com.sudo.railo.payment.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.payment.application.dto.request.PaymentProcessAccountRequest;
import com.sudo.railo.payment.application.dto.request.PaymentProcessCardRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCancelResponse;
import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentProcessResponse;
import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.domain.type.PaymentMethod;
import com.sudo.railo.payment.exception.PaymentError;
import com.sudo.railo.payment.infrastructure.PaymentRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.fixture.PaymentFixture;
import com.sudo.railo.support.helper.ReservationTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper.TrainScheduleWithStopStations;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.domain.Train;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
class PaymentServiceTest {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private ReservationTestHelper reservationTestHelper;

	private Member member;

	private Reservation reservation;

	@BeforeEach
	void beforeEach() {
		member = MemberFixture.createStandardMember();
		memberRepository.save(member);

		Train train = trainTestHelper.createKTX();
		TrainScheduleWithStopStations scheduleWithStops = trainScheduleTestHelper.createSchedule(train);
		reservation = reservationTestHelper.createReservation(member, scheduleWithStops);
	}

	@Test
	@DisplayName("카드 결제가 성공한다")
	void processPaymentViaCard_success() {
		// given
		PaymentProcessCardRequest request = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));

		// when
		PaymentProcessResponse response = paymentService
			.processPaymentViaCard(member.getMemberDetail().getMemberNo(), request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.paymentKey()).isNotNull();
		assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(reservation.getFare()));
		assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);

		// 결제 엔티티 확인
		Payment savedPayment = paymentRepository.findByPaymentKey(response.paymentKey())
			.orElseThrow(() -> new AssertionError("결제가 DB에 저장되지 않았습니다"));
		assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(savedPayment.getPaidAt()).isNotNull();

		// 예약 상태 확인
		Reservation updatedReservation = reservationRepository.findById(reservation.getId())
			.orElseThrow(() -> new AssertionError("예약을 찾을 수 없습니다"));
		assertThat(updatedReservation.getReservationStatus()).isEqualTo(ReservationStatus.PAID);
	}

	@Test
	@DisplayName("계좌이체 결제가 성공한다")
	void processPaymentViaBankAccount_success() {
		// given
		PaymentProcessAccountRequest request = PaymentFixture.createAccountPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));

		// when
		PaymentProcessResponse response = paymentService
			.processPaymentViaBankAccount(member.getMemberDetail().getMemberNo(), request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.paymentKey()).isNotNull();
		assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(reservation.getFare()));
		assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.TRANSFER);
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);

		// 결제 엔티티 확인
		Payment savedPayment = paymentRepository.findByPaymentKey(response.paymentKey())
			.orElseThrow(() -> new AssertionError("결제가 DB에 저장되지 않았습니다"));
		assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(savedPayment.getPaidAt()).isNotNull();

		// 예약 상태 확인
		Reservation updatedReservation = reservationRepository.findById(reservation.getId())
			.orElseThrow(() -> new AssertionError("예약을 찾을 수 없습니다"));
		assertThat(updatedReservation.getReservationStatus()).isEqualTo(ReservationStatus.PAID);
	}

	@Test
	@DisplayName("금액이 일치하지 않으면 결제가 실패한다")
	void processPayment_fail_whenAmountMismatch() {
		// given
		PaymentProcessCardRequest request = PaymentFixture
			.createCardPaymentRequest(reservation.getId(), BigDecimal.valueOf(999999));

		// when & then
		assertThatThrownBy(() -> paymentService
			.processPaymentViaCard(member.getMemberDetail().getMemberNo(), request))
			.isInstanceOf(BusinessException.class).hasMessage(PaymentError.PAYMENT_AMOUNT_MISMATCH.getMessage());
	}

	@Test
	@DisplayName("다른 사용자의 예약으로는 결제할 수 없다")
	void processPayment_fail_whenNotOwner() {
		// given
		Member other = MemberFixture.createOtherMember();
		memberRepository.save(other);

		PaymentProcessCardRequest request = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));

		// when & then
		assertThatThrownBy(() -> paymentService
			.processPaymentViaCard(other.getMemberDetail().getMemberNo(), request))
			.isInstanceOf(BusinessException.class).hasMessage(PaymentError.RESERVATION_ACCESS_DENIED.getMessage());
	}

	@Test
	@DisplayName("이미 결제된 예약은 중복 결제할 수 없다")
	void processPayment_fail_whenAlreadyPaid() {
		// given
		// 첫 번째 결제
		PaymentProcessCardRequest firstRequest = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));
		paymentService.processPaymentViaCard(member.getMemberDetail().getMemberNo(), firstRequest);

		// 두 번째 결제 시도
		PaymentProcessCardRequest secondRequest = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));

		// when & then
		// 첫 번째 결제 후 예약 상태가 PAID로 변경되어 결제할 수 없는 상태가 됨
		assertThatThrownBy(() -> paymentService
			.processPaymentViaCard(member.getMemberDetail().getMemberNo(), secondRequest))
			.isInstanceOf(BusinessException.class).hasMessage(PaymentError.RESERVATION_NOT_PAYABLE.getMessage());
	}

	@Test
	@DisplayName("결제 취소가 성공한다")
	void cancelPayment_success() {
		// given
		PaymentProcessCardRequest request = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));
		PaymentProcessResponse paymentResponse = paymentService
			.processPaymentViaCard(member.getMemberDetail().getMemberNo(), request);

		// when
		PaymentCancelResponse cancelResponse = paymentService
			.cancelPayment(member.getMemberDetail().getMemberNo(), paymentResponse.paymentKey());

		// then
		assertThat(cancelResponse).isNotNull();
		assertThat(cancelResponse.paymentKey()).isEqualTo(paymentResponse.paymentKey());
		assertThat(cancelResponse.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(cancelResponse.cancelledAt()).isNotNull();

		// 결제 엔티티 확인
		Payment cancelledPayment = paymentRepository.findByPaymentKey(paymentResponse.paymentKey())
			.orElseThrow(() -> new AssertionError("결제를 찾을 수 없습니다"));
		assertThat(cancelledPayment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

		// 예약 상태 확인
		Reservation cancelledReservation = reservationRepository.findById(reservation.getId())
			.orElseThrow(() -> new AssertionError("예약을 찾을 수 없습니다"));
		assertThat(cancelledReservation.getReservationStatus()).isEqualTo(ReservationStatus.REFUNDED);
	}

	@Test
	@DisplayName("다른 사용자의 결제는 취소할 수 없다")
	void cancelPayment_fail_whenNotOwner() {
		// given
		Member other = MemberFixture.createOtherMember();
		memberRepository.save(other);

		PaymentProcessCardRequest request = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));
		PaymentProcessResponse paymentResponse = paymentService
			.processPaymentViaCard(member.getMemberDetail().getMemberNo(), request);

		// when & then
		assertThatThrownBy(() -> paymentService.cancelPayment(
			other.getMemberDetail().getMemberNo(), paymentResponse.paymentKey()))
			.isInstanceOf(BusinessException.class).hasMessage(PaymentError.PAYMENT_ACCESS_DENIED.getMessage());
	}

	@Test
	@DisplayName("결제 내역 조회가 성공한다")
	void getPaymentHistory_success() {
		// given
		// 카드 결제만 진행 (StationFare 중복 생성 문제 회피)
		PaymentProcessCardRequest cardRequest = PaymentFixture.createCardPaymentRequest(
			reservation.getId(), BigDecimal.valueOf(reservation.getFare()));
		paymentService.processPaymentViaCard(member.getMemberDetail().getMemberNo(), cardRequest);

		// when
		List<PaymentHistoryResponse> paymentHistory =
			paymentService.getPaymentHistory(member.getMemberDetail().getMemberNo());

		// then
		assertThat(paymentHistory).hasSize(1);

		PaymentHistoryResponse cardPayment = paymentHistory.get(0);
		assertThat(cardPayment.paymentMethod()).isEqualTo(PaymentMethod.CARD);
		assertThat(cardPayment.paymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(cardPayment.amount()).isEqualByComparingTo(BigDecimal.valueOf(reservation.getFare()));
		assertThat(cardPayment.paymentKey()).isNotNull();
		assertThat(cardPayment.reservationCode()).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 회원의 결제 내역 조회 시 예외가 발생한다")
	void getPaymentHistory_fail_whenMemberNotFound() {
		// when & then
		assertThatThrownBy(() -> paymentService.getPaymentHistory("nonexistent"))
			.isInstanceOf(BusinessException.class).hasMessage("사용자를 찾을 수 없습니다.");
	}
}
