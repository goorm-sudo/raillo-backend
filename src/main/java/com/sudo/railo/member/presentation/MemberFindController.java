package com.sudo.railo.member.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.auth.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.auth.success.AuthSuccess;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberFindService;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;
import com.sudo.railo.member.docs.MemberFindControllerDocs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberFindController implements MemberFindControllerDocs {

	private final MemberFindService memberFindService;

	/**
	 * 이메일 인증을 통한 회원 번호 찾기
	 * */
	@PostMapping("/member-no")
	public SuccessResponse<SendCodeResponse> requestFindMemberNo(@RequestBody @Valid FindMemberNoRequest request) {

		SendCodeResponse response = memberFindService.requestFindMemberNo(request);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	@PostMapping("/member-no/verify")
	public SuccessResponse<VerifyMemberNoResponse> verifyFindMemberNo(@RequestBody @Valid VerifyCodeRequest request) {

		VerifyMemberNoResponse response = memberFindService.verifyFindMemberNo(request);

		return SuccessResponse.of(AuthSuccess.VERIFY_CODE_SUCCESS, response);
	}

	/**
	 * 이메일 인증을 통한 비밀번호 찾기
	 * */
	@PostMapping("/password")
	public SuccessResponse<SendCodeResponse> requestFindPassword(@RequestBody @Valid FindPasswordRequest request) {

		SendCodeResponse response = memberFindService.requestFindPassword(request);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	@PostMapping("/password/verify")
	public SuccessResponse<TemporaryTokenResponse> verifyFindPassword(@RequestBody @Valid VerifyCodeRequest request) {

		TemporaryTokenResponse response = memberFindService.verifyFindPassword(request);

		return SuccessResponse.of(AuthSuccess.VERIFY_CODE_SUCCESS, response);
	}
}
