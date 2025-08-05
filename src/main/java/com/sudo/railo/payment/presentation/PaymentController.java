package com.sudo.railo.payment.presentation;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.payment.application.PaymentService;
import com.sudo.railo.payment.application.dto.request.PaymentProcessAccountRequest;
import com.sudo.railo.payment.application.dto.request.PaymentProcessCardRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCancelResponse;
import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentProcessResponse;
import com.sudo.railo.payment.success.PaymentSuccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payments", description = "결제 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;

	@Operation(summary = "결제 처리 (카드)", description = "예약에 대한 결제를 카드를 이용해 처리합니다.")
	@PostMapping("/card")
	public SuccessResponse<PaymentProcessResponse> processPaymentViaCard(
			@Valid @RequestBody PaymentProcessCardRequest request,
			@AuthenticationPrincipal UserDetails userDetails) {

		String memberNo = userDetails.getUsername();
		PaymentProcessResponse response = paymentService.processPaymentViaCard(memberNo, request);

		return SuccessResponse.of(PaymentSuccess.PAYMENT_PROCESS_SUCCESS, response);
	}

	@Operation(summary = "결제 처리 (계좌이체)", description = "예약에 대한 결제를 은행 계좌를 이용해 처리합니다.")
	@PostMapping("/bank-account")
	public SuccessResponse<PaymentProcessResponse> processPaymentViaBankAccount(
			@Valid @RequestBody PaymentProcessAccountRequest request,
			@AuthenticationPrincipal UserDetails userDetails) {

		String memberNo = userDetails.getUsername();
		PaymentProcessResponse response =
				paymentService.processPaymentViaBankAccount(memberNo, request);

		return SuccessResponse.of(PaymentSuccess.PAYMENT_PROCESS_SUCCESS, response);
	}

	@Operation(summary = "결제 취소", description = "완료된 결제를 취소 및 환불처리 합니다.")
	@PostMapping("/{paymentKey}/cancel")
	public SuccessResponse<PaymentCancelResponse> cancelPayment(@PathVariable String paymentKey,
		@AuthenticationPrincipal UserDetails userDetails) {
		String memberNo = userDetails.getUsername();
		PaymentCancelResponse response = paymentService.cancelPayment(memberNo, paymentKey);

		return SuccessResponse.of(PaymentSuccess.PAYMENT_CANCEL_SUCCESS, response);
	}

	@Operation(summary = "결제 내역 조회", description = "사용자의 결제 내역을 조회합니다.")
	@GetMapping
	public SuccessResponse<List<PaymentHistoryResponse>> getPaymentHistory(
		@AuthenticationPrincipal UserDetails userDetails) {

		String memberNo = userDetails.getUsername();
		List<PaymentHistoryResponse> response = paymentService.getPaymentHistory(memberNo);

		return SuccessResponse.of(PaymentSuccess.PAYMENT_HISTORY_SUCCESS, response);
	}
}
