package com.sudo.railo.payment.application;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.booking.application.TicketService;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.payment.application.dto.PaymentInfo;
import com.sudo.railo.payment.application.dto.request.PaymentProcessAccountRequest;
import com.sudo.railo.payment.application.dto.request.PaymentProcessCardRequest;
import com.sudo.railo.payment.application.dto.request.PaymentProcessRequest;
import com.sudo.railo.payment.application.dto.response.PaymentProcessResponse;
import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.exception.PaymentError;
import com.sudo.railo.payment.infrastructure.PaymentRepository;
import com.sudo.railo.payment.util.PaymentKeyGenerator;
import com.sudo.railo.train.infrastructure.SeatReservationRepositoryCustom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final ReservationRepository reservationRepository;
	private final SeatReservationRepositoryCustom seatReservationRepositoryCustom;
	private final MemberRepository memberRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentKeyGenerator paymentKeyGenerator;
	private final TicketService ticketService;

	/**
	 * 결제 처리 (카드)
	 *
	 * @param memberNo 회원번호
	 * @param request {@link PaymentProcessCardRequest} 객체
	 * @return {@link PaymentProcessResponse} 객체
	 */
	@Transactional
	public PaymentProcessResponse processPaymentViaCard(String memberNo, PaymentProcessCardRequest request) {
		return processPayment(memberNo, request);
	}

	/**
	 * 결제 처리 (계좌 이체)
	 *
	 * @param memberNo 회원번호
	 * @param request {@link PaymentProcessAccountRequest} 객체
	 * @return {@link PaymentProcessResponse} 객체
	 */
	@Transactional
	public PaymentProcessResponse processPaymentViaBankAccount(String memberNo, PaymentProcessAccountRequest request) {
		return processPayment(memberNo, request);
	}

	/**
	 * 결제 처리 (공통)
	 *
	 * @param memberNo 회원번호
	 * @param request {@link PaymentProcessRequest} 객체
	 * @return {@link PaymentProcessResponse} 객체
	 */
	private PaymentProcessResponse processPayment(String memberNo, PaymentProcessRequest request) {
		Reservation reservation = getReservation(request.getReservationId());
		Payment payment = createAndSavePayment(request, memberNo, reservation);

		validatePaymentApprovalConditions(payment, reservation);
		executePaymentApproval(payment, reservation);
		completePaymentProcess(request, payment, reservation);

		return PaymentProcessResponse.from(payment);
	}

	private Reservation getReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(PaymentError.RESERVATION_NOT_FOUND));
	}

	private Payment createAndSavePayment(PaymentProcessRequest request, String memberNo, Reservation reservation) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		validateReservation(member, reservation);
		validatePaymentProcessRequest(request, reservation);

		String paymentKey = paymentKeyGenerator.generatePaymentKey(memberNo);
		PaymentInfo paymentInfo = new PaymentInfo(request.getAmount(), request.getPaymentMethod(), PaymentStatus.PENDING);
		Payment payment = Payment.create(member, reservation, paymentKey, paymentInfo);

		return paymentRepository.save(payment);
	}

	private void validateReservation(Member member, Reservation reservation) {
		// 예약 소유자 검증
		if (!reservation.getMember().getId().equals(member.getId())) {
			throw new BusinessException(PaymentError.RESERVATION_ACCESS_DENIED);
		}

		// 예약 상태 검증 (결제 가능한 상태인지)
		if (!reservation.canBePaid()) {
			throw new BusinessException(PaymentError.RESERVATION_NOT_PAYABLE);
		}
	}

	private void validatePaymentProcessRequest(PaymentProcessRequest request, Reservation reservation) {
		// 금액 유효성 검증
		if (request.getAmount() == null) {
			throw new BusinessException(PaymentError.INVALID_PAYMENT_AMOUNT);
		}

		// 금액 위변조 검증
		if (!request.getAmount().equals(BigDecimal.valueOf(reservation.getFare()))) {
			throw new BusinessException(PaymentError.PAYMENT_AMOUNT_MISMATCH);
		}

		// 중복 결제 검증
		if (paymentRepository.existsByReservationIdAndPaymentStatus(request.getReservationId(), PaymentStatus.PAID)) {
			throw new BusinessException(PaymentError.PAYMENT_ALREADY_COMPLETED);
		}
	}

	private void validatePaymentApprovalConditions(Payment payment, Reservation reservation) {
		// 결제 상태 검증
		if (!payment.canBePaid()) {
			throw new BusinessException(PaymentError.PAYMENT_NOT_APPROVABLE);
		}

		// 예약 상태 재검증 (동시성 문제 방지)
		if (!reservation.canBePaid()) {
			throw new BusinessException(PaymentError.RESERVATION_NOT_PAYABLE);
		}
	}

	private void executePaymentApproval(Payment payment, Reservation reservation) {
		// 결제 승인 처리
		payment.approve();

		// 예약 상태 변경
		markReservationAsPaid(reservation);
	}

	private void markReservationAsPaid(Reservation reservation) {
		reservation.approve();
		reservationRepository.save(reservation);

		log.info("예약 결제 완료 처리: reservationId={}", reservation.getId());
	}

	private void completePaymentProcess(PaymentProcessRequest request, Payment payment, Reservation reservation) {
		// 티켓 발급
		generateTicket(reservation);

		log.info("결제 완료: paymentKey={}, reservationId={}, amount={}",
			payment.getPaymentKey(), request.getReservationId(), request.getAmount());
	}

	private void generateTicket(Reservation reservation) {
		seatReservationRepositoryCustom.findSeatInfoByReservationId(reservation.getId())
			.forEach(seatInfoProjection -> ticketService.createTicket(
				reservation, seatInfoProjection.getSeat(), seatInfoProjection.getPassengerType()));
	}
}
