package com.sudo.railo.booking.application.event;

import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.infra.JpaReservationRepository;
import com.sudo.railo.booking.infra.JpaTicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 이벤트 리스너 (Booking 도메인)
 *
 * PaymentEventTranslator가 변환한 Booking 전용 이벤트를 수신하여
 * Reservation과 Ticket 상태를 업데이트합니다.
 *
 * Event Translator 패턴을 통해 Payment 도메인과의 결합도를 낮추고,
 * Booking 도메인에 필요한 정보만 전달받습니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final JpaReservationRepository reservationRepository;
    private final JpaTicketRepository ticketRepository;

    /**
     * 결제 완료 이벤트 처리
     * Reservation: RESERVED -> PAID
     * Ticket: RESERVED -> PAID
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handlePaymentCompleted(BookingPaymentCompletedEvent event) {
		log.info("결제 완료 이벤트 수신 - paymentId: {}, reservationId: {}",
			event.getPaymentId(), event.getReservationId());

        try {
			Long reservationId = event.getReservationId();

			log.info("예약 상태 업데이트 시작 - reservationId: {}", reservationId);

            // 1. Reservation 상태 업데이트: RESERVED -> PAID
            updateReservationToPaid(reservationId);

            // 2. 관련 Ticket들 상태 업데이트: RESERVED -> PAID
            updateTicketsToPaid(reservationId);

			log.info("예약 상태 업데이트 완료 - reservationId: {}, 상태: PAID", reservationId);

        } catch (Exception e) {
			log.error("이벤트 처리 중 오류 발생 - paymentId: {}, reservationId: {}",
				event.getPaymentId(), event.getReservationId(), e);
            // 이벤트 처리 실패는 메인 결제 트랜잭션에 영향주지 않음
            // 별도 보상 작업이나 재시도 로직 필요 시 추가
        }
    }

    /**
     * 결제 취소 이벤트 처리
     * Reservation: RESERVED -> CANCELLED
     * Ticket: RESERVED -> CANCELLED
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handlePaymentCancelled(BookingPaymentCancelledEvent event) {
        log.info("결제 취소 이벤트 수신 - paymentId: {}, reservationId: {}",
                event.getPaymentId(), event.getReservationId());

        try {
            Long reservationId = event.getReservationId();

            // 1. Reservation 상태 업데이트: RESERVED -> CANCELLED
            updateReservationToCancelled(reservationId);

            // 2. 관련 Ticket들 상태 업데이트: RESERVED -> CANCELLED
            updateTicketsToCancelled(reservationId);

            log.info("결제 취소 이벤트 처리 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("결제 취소 이벤트 처리 중 오류 발생 - paymentId: {}", event.getPaymentId(), e);
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * Reservation: RESERVED -> CANCELLED
     * Ticket: RESERVED -> CANCELLED
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(BookingPaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신 - paymentId: {}, reservationId: {}, 실패 사유: {}",
                event.getPaymentId(), event.getReservationId(), event.getFailureReason());

        try {
            Long reservationId = event.getReservationId();

            // 1. Reservation 상태 업데이트: RESERVED -> CANCELLED (실패한 예약은 취소 처리)
            updateReservationToCancelled(reservationId);

            // 2. 관련 Ticket들 상태 업데이트: RESERVED -> CANCELLED
            updateTicketsToCancelled(reservationId);

            log.info("결제 실패 이벤트 처리 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("결제 실패 이벤트 처리 중 오류 발생 - paymentId: {}", event.getPaymentId(), e);
        }
    }

    /**
     * 결제 환불 이벤트 처리
     * Reservation: PAID -> REFUNDED
     * Ticket: PAID -> REFUNDED
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handlePaymentRefunded(BookingPaymentRefundedEvent event) {
        log.info("결제 환불 이벤트 수신 - paymentId: {}, reservationId: {}",
                event.getPaymentId(), event.getReservationId());

        try {
            Long reservationId = event.getReservationId();

            // 1. Reservation 상태 업데이트: PAID -> REFUNDED
            updateReservationToRefunded(reservationId);

            // 2. 관련 Ticket들 상태 업데이트: PAID -> REFUNDED
            updateTicketsToRefunded(reservationId);

            log.info("결제 환불 이벤트 처리 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("결제 환불 이벤트 처리 중 오류 발생 - paymentId: {}", event.getPaymentId(), e);
        }
    }

    /**
     * Reservation 상태를 PAID로 업데이트
     */
    private void updateReservationToPaid(Long reservationId) {
        // JPA 리포지토리의 findById 메서드 직접 사용
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));

        // 디버깅: 예약 객체 타입 및 메서드 확인
        log.debug("예약 객체 타입: {}", reservation.getClass().getName());
        log.debug("예약 객체 메서드 목록: {}", Arrays.toString(reservation.getClass().getMethods()));

        reservation.markAsPaid();
        reservationRepository.save(reservation);

        log.debug("예약 상태 업데이트 완료 - reservationId: {}, status: PAID", reservationId);
    }

    /**
     * 관련 Ticket들 상태를 PAID로 업데이트 (개별 저장)
     */
    private void updateTicketsToPaid(Long reservationId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        for (Ticket ticket : tickets) {
            ticket.markAsPaid();
            ticketRepository.save(ticket); // 개별 저장
        }

        log.debug("티켓 상태 업데이트 완료 - reservationId: {}, ticketCount: {}, status: PAID",
                reservationId, tickets.size());
    }

    /**
     * Reservation 상태를 CANCELLED로 업데이트
     */
    private void updateReservationToCancelled(Long reservationId) {
        // JPA 리포지토리의 findById 메서드 직접 사용
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));

        reservation.markAsCancelled();
        reservationRepository.save(reservation);

        log.debug("예약 상태 업데이트 완료 - reservationId: {}, status: CANCELLED", reservationId);
    }

    /**
     * 관련 Ticket들 상태를 CANCELLED로 업데이트 (개별 저장)
     */
    private void updateTicketsToCancelled(Long reservationId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        for (Ticket ticket : tickets) {
            ticket.markAsCancelled();
            ticketRepository.save(ticket); // 개별 저장
        }

        log.debug("티켓 상태 업데이트 완료 - reservationId: {}, ticketCount: {}, status: CANCELLED",
                reservationId, tickets.size());
    }

    /**
     * Reservation 상태를 REFUNDED로 업데이트
     */
    private void updateReservationToRefunded(Long reservationId) {
        // JPA 리포지토리의 findById 메서드 직접 사용
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));

        reservation.markAsRefunded();
        reservationRepository.save(reservation);

        log.debug("예약 상태 업데이트 완료 - reservationId: {}, status: REFUNDED", reservationId);
    }

    /**
     * 관련 Ticket들 상태를 REFUNDED로 업데이트 (개별 저장)
     */
    private void updateTicketsToRefunded(Long reservationId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        for (Ticket ticket : tickets) {
            ticket.markAsRefunded();
            ticketRepository.save(ticket); // 개별 저장
        }

        log.debug("티켓 상태 업데이트 완료 - reservationId: {}, ticketCount: {}, status: REFUNDED",
                reservationId, tickets.size());
    }
}
