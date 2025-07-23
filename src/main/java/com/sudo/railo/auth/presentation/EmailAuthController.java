package com.sudo.railo.auth.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.auth.application.EmailAuthService;
import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.auth.application.dto.request.SendCodeRequest;
import com.sudo.railo.auth.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.application.dto.response.VerifyCodeResponse;
import com.sudo.railo.auth.docs.EmailAuthControllerDocs;
import com.sudo.railo.auth.success.AuthSuccess;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class EmailAuthController implements EmailAuthControllerDocs {

	private final EmailAuthService emailAuthService;
	private final MemberService memberService;

	/**
	 * 이메일 코드 전송 - 인증되지 않은 사용자
	 * */
	@PostMapping("/emails")
	public SuccessResponse<SendCodeResponse> sendAuthCode(@RequestBody @Valid SendCodeRequest request) {

		String email = request.email();
		SendCodeResponse response = emailAuthService.sendAuthCode(email);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	/**
	 * 이메일 코드 전송 - 인증된 사용자
	 * */
	@PostMapping("/members/emails")
	public SuccessResponse<SendCodeResponse> sendAuthCodeWithMember(
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		log.info("memberNo: {}", memberNo);

		String email = memberService.getMemberEmail(memberNo);
		SendCodeResponse response = emailAuthService.sendAuthCode(email);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	/**
	 * 이메일 코드 검증 - 인증된 사용자, 인증되지 않은 사용자 모두 사용
	 * */
	@PostMapping("/emails/verify")
	public SuccessResponse<VerifyCodeResponse> verifyAuthCode(@RequestBody @Valid VerifyCodeRequest request) {

		String email = request.email();
		String authCode = request.authCode();

		boolean isVerified = emailAuthService.verifyAuthCode(email, authCode);
		VerifyCodeResponse response = new VerifyCodeResponse(isVerified);

		return SuccessResponse.of(AuthSuccess.VERIFY_CODE_SUCCESS_FINISH, response);
	}

}
