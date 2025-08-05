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
@Schema(description = "결제 처리 요청 (계좌이체)")
public class PaymentProcessAccountRequest extends PaymentProcessRequest {

	@Schema(description = "은행 코드", example = "088", allowableValues = {"088", "020", "003", "004", "011"})
	@NotNull(message = "은행 코드는 필수입니다")
	private String bankCode;

	@Schema(description = "계좌 번호", example = "123456789012")
	@NotNull(message = "계좌 번호는 필수입니다")
	private String accountNumber;

	@Schema(description = "예금주명", example = "홍길동")
	@NotNull(message = "예금주명은 필수입니다")
	private String accountHolderName;

	@Schema(description = "주민등록번호 (앞 6자리)", example = "000505")
	@NotNull(message = "인증 번호는 필수입니다")
	private String identificationNumber;

	@Schema(description = "계좌 비밀번호 (앞 2자리)", example = "75")
	@NotNull(message = "계좌 비밀번호는 필수입니다")
	private String accountPassword;

	@Override
	public PaymentMethod getPaymentMethod() {
		return PaymentMethod.TRANSFER;
	}
}
