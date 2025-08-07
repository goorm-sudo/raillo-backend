package com.sudo.railo.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.payment.application.dto.PaymentInfo;
import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.domain.type.PaymentMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	@Comment("결제 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@Comment("멤버 ID")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	@Comment("예약 ID")
	private Reservation reservation;

	@Column(name = "payment_key", nullable = false, unique = true)
	@Comment("결제 고유번호")
	private String paymentKey;

	@Column(name = "amount", nullable = false)
	@Comment("결제 금액")
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false)
	@Comment("결제 상태")
	private PaymentStatus paymentStatus;

	@Column(name = "paid_at")
	@Comment("결제 일자")
	private LocalDateTime paidAt;

	@Column(name = "cancelled_at")
	@Comment("결제 취소 일자")
	private LocalDateTime cancelledAt;

	@Column(name = "refunded_at")
	@Comment("환불 처리 일자")
	private LocalDateTime refundedAt;

	@Column(name = "failure_reason")
	@Comment("결제 실패 사유")
	private String failureReason;

	private Payment(Member member, Reservation reservation, String paymentKey, BigDecimal amount,
		PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
		this.member = member;
		this.reservation = reservation;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
	}

	public static Payment create(Member member, Reservation reservation, String paymentKey,
		PaymentInfo paymentInfo) {
		return new Payment(member, reservation, paymentKey, paymentInfo.amount(),
			paymentInfo.paymentMethod(), paymentInfo.paymentStatus());
	}

	// 결제 승인
	public void approve() {
		this.paymentStatus = PaymentStatus.PAID;
		this.paidAt = LocalDateTime.now();
	}

	// 결제 취소
	public void cancel(String reason) {
		this.paymentStatus = PaymentStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
		this.failureReason = reason;
	}

	// 환불 처리
	public void refund() {
		this.paymentStatus = PaymentStatus.REFUNDED;
		this.refundedAt = LocalDateTime.now();
	}

	// 결제 실패
	public void fail(String reason) {
		this.paymentStatus = PaymentStatus.FAILED;
		this.failureReason = reason;
	}

	// 결제 가능 여부 확인
	public boolean canBePaid() {
		return this.paymentStatus.isPayable();
	}

	// 취소 가능 여부 확인
	public boolean canBeCancelled() {
		return this.paymentStatus.isCancellable();
	}

	// 환불 가능 여부 확인
	public boolean canBeRefunded() {
		return this.paidAt != null && this.paymentStatus.isRefundable();
	}
}
