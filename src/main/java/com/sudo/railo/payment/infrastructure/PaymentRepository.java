package com.sudo.railo.payment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {

	/**
	 * 결제 키로 결제 정보 조회
	 */
	Optional<Payment> findByPaymentKey(String paymentKey);

	/**
	 * 회원의 모든 결제 목록 조회 (최신순)
	 */
	List<Payment> findByMemberIdOrderByPaidAtDesc(Long memberId);

	/**
	 * 예약 ID와 결제 상태로 결제 존재 여부 확인
	 */
	boolean existsByReservationIdAndPaymentStatus(Long reservationId, PaymentStatus paymentStatus);


}
