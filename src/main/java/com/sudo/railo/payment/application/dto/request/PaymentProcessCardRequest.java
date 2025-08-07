package com.sudo.railo.payment.application.dto.request;

import com.sudo.railo.payment.domain.type.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Schema(description = "결제 처리 요청 (카드)")
public class PaymentProcessCardRequest extends PaymentProcessRequest {

	@Schema(description = "카드 번호", example = "1234-5678-9012-3456")
	@NotNull(message = "카드 번호는 필수입니다")
	private String cardNumber;

	@Schema(description = "유효기간 (MMYY)", example = "1225")
	@NotNull(message = "유효 기간은 필수입니다")
	private String validThru;

	@Schema(description = "주민등록번호 (앞 6자리)", example = "000505")
	@NotNull(message = "인증 번호는 필수입니다")
	String rrn;

	@Schema(description = "할부 개월수", example = "0", allowableValues = {"0", "2", "3", "6", "12"})
	@NotNull(message = "할부 개월수는 필수입니다")
	private Integer installmentMonths;

	@Schema(description = "카드 비밀번호 (앞 2자리)", example = "12")
	@NotNull(message = "카드 비밀번호는 필수입니다")
	private int cardPassword;

	@Override
	public PaymentMethod getPaymentMethod() {
		return PaymentMethod.CARD;
	}
}
