package com.sudo.railo.payment.infrastructure;

import java.util.List;

import com.sudo.railo.payment.application.dto.projection.PaymentProjection;

public interface PaymentRepositoryCustom {

	/**
	 * 회원의 결제 히스토리를 프로젝션으로 조회 (가장 효율적)
	 *
	 * @param memberId 회원 ID
	 * @return PaymentHistoryResponse 리스트 (필요한 필드만 조회)
	 */
	List<PaymentProjection> findPaymentHistoryByMemberId(Long memberId);
}
