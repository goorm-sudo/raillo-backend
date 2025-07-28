package com.sudo.railo.payment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/**
	 * 결제 키로 결제 정보 조회
	 */
	Optional<Payment> findByPaymentKey(String paymentKey);

	/**
	 * 예약 ID로 결제 정보 조회
	 */
	Optional<Payment> findByReservationId(Long reservationId);

	/**
	 * 회원 ID와 결제 상태로 결제 목록 조회
	 */
	List<Payment> findByMemberIdAndPaymentStatusOrderByPaidAtDesc(Long memberId, PaymentStatus paymentStatus);

	/**
	 * 회원의 모든 결제 목록 조회 (최신순)
	 */
	List<Payment> findByMemberIdOrderByPaidAtDesc(Long memberId);

	/**
	 * 예약 ID와 결제 상태로 결제 존재 여부 확인
	 */
	boolean existsByReservationIdAndPaymentStatus(Long reservationId, PaymentStatus paymentStatus);

	/**
	 * 특정 기간 동안의 결제 완료 건수 조회
	 */
	@Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = :status AND p.paidAt BETWEEN :startDate AND :endDate")
	long countByPaymentStatusAndPaidAtBetween(@Param("status") PaymentStatus status,
		@Param("startDate") java.time.LocalDateTime startDate,
		@Param("endDate") java.time.LocalDateTime endDate);
}
