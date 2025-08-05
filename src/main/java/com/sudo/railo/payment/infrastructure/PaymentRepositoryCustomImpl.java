package com.sudo.railo.payment.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;
import static com.sudo.railo.member.domain.QMember.*;
import static com.sudo.railo.payment.domain.QPayment.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.payment.application.dto.projection.PaymentProjection;
import com.sudo.railo.payment.application.dto.projection.QPaymentProjection;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<PaymentProjection> findPaymentHistoryByMemberId(Long memberId) {
		return queryFactory
			.select(new QPaymentProjection(
				payment.id,
				payment.paymentKey,
				reservation.reservationCode,
				payment.amount,
				payment.paymentMethod,
				payment.paymentStatus,
				payment.paidAt,
				payment.cancelledAt,
				payment.refundedAt))
			.from(payment)
			.join(payment.member, member)
			.join(payment.reservation, reservation)
			.where(payment.member.id.eq(memberId))
			.orderBy(payment.paidAt.desc()).fetch();
	}
}
