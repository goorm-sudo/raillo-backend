package com.sudo.railo.payment.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.payment.application.PaymentService;
import com.sudo.railo.payment.application.dto.request.PaymentProcessRequest;
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

	@Operation(summary = "결제 처리", description = "예약에 대한 결제를 즉시 처리합니다.")
	@PostMapping
	public SuccessResponse<PaymentProcessResponse> processPayment(
		@Valid @RequestBody PaymentProcessRequest request,
		@AuthenticationPrincipal UserDetails userDetails) {

		String memberNo = userDetails.getUsername();
		PaymentProcessResponse response = paymentService.processPayment(memberNo, request);

		return SuccessResponse.of(PaymentSuccess.PAYMENT_PROCESS_SUCCESS, response);
	}
}
