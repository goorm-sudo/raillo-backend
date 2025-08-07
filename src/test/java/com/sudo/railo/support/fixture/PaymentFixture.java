package com.sudo.railo.support.fixture;

import java.math.BigDecimal;

import com.sudo.railo.payment.application.dto.request.PaymentProcessAccountRequest;
import com.sudo.railo.payment.application.dto.request.PaymentProcessCardRequest;

import lombok.Getter;

@Getter
public enum PaymentFixture {

	CARD_PAYMENT("1234-5678-9012-3456", "1225", "000505", 0, 12),

	ACCOUNT_PAYMENT("088", "123456789012", "홍길동", "000505", "75");

	// 카드 결제 관련 필드
	private final String cardNumber;
	private final String validThru;
	private final String rrn;
	private final Integer installmentMonths;
	private final Integer cardPassword;

	// 계좌이체 관련 필드
	private final String bankCode;
	private final String accountNumber;
	private final String accountHolderName;
	private final String identificationNumber;
	private final String accountPassword;

	PaymentFixture(String cardNumber, String validThru, String rrn, Integer installmentMonths,
		Integer cardPassword) {
		this.cardNumber = cardNumber;
		this.validThru = validThru;
		this.rrn = rrn;
		this.installmentMonths = installmentMonths;
		this.cardPassword = cardPassword;
		// 계좌이체 필드는 null로 초기화
		this.bankCode = null;
		this.accountNumber = null;
		this.accountHolderName = null;
		this.identificationNumber = null;
		this.accountPassword = null;
	}

	PaymentFixture(String bankCode, String accountNumber, String accountHolderName,
		String identificationNumber, String accountPassword) {
		this.bankCode = bankCode;
		this.accountNumber = accountNumber;
		this.accountHolderName = accountHolderName;
		this.identificationNumber = identificationNumber;
		this.accountPassword = accountPassword;
		// 카드 결제 필드는 null로 초기화
		this.cardNumber = null;
		this.validThru = null;
		this.rrn = null;
		this.installmentMonths = null;
		this.cardPassword = null;
	}

	public static PaymentProcessCardRequest createCardPaymentRequest(Long reservationId,
		BigDecimal amount) {
		PaymentProcessCardRequest request = new PaymentProcessCardRequest();
		request.setReservationId(reservationId);
		request.setAmount(amount);
		request.setCardNumber(CARD_PAYMENT.cardNumber);
		request.setValidThru(CARD_PAYMENT.validThru);
		request.setRrn(CARD_PAYMENT.rrn);
		request.setInstallmentMonths(CARD_PAYMENT.installmentMonths);
		request.setCardPassword(CARD_PAYMENT.cardPassword);
		return request;
	}

	public static PaymentProcessAccountRequest createAccountPaymentRequest(Long reservationId,
		BigDecimal amount) {
		PaymentProcessAccountRequest request = new PaymentProcessAccountRequest();
		request.setReservationId(reservationId);
		request.setAmount(amount);
		request.setBankCode(ACCOUNT_PAYMENT.bankCode);
		request.setAccountNumber(ACCOUNT_PAYMENT.accountNumber);
		request.setAccountHolderName(ACCOUNT_PAYMENT.accountHolderName);
		request.setIdentificationNumber(ACCOUNT_PAYMENT.identificationNumber);
		request.setAccountPassword(ACCOUNT_PAYMENT.accountPassword);
		return request;
	}
}
