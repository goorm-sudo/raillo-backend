package com.sudo.railo.member.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberUpdateService;
import com.sudo.railo.member.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.docs.MemberUpdateControllerDocs;
import com.sudo.railo.member.success.AuthSuccess;
import com.sudo.railo.member.success.MemberSuccess;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberUpdateController implements MemberUpdateControllerDocs {

	private final MemberUpdateService memberUpdateService;

	@PostMapping("auth/members/me/email-code")
	public SuccessResponse<SendCodeResponse> requestUpdateEmail(@RequestBody @Valid SendCodeRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		SendCodeResponse response = memberUpdateService.requestUpdateEmail(request, memberNo);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	@PutMapping("auth/members/me/email-code")
	public SuccessResponse<?> verifyUpdateEmail(@RequestBody @Valid UpdateEmailRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		memberUpdateService.verifyUpdateEmail(request, memberNo);

		return SuccessResponse.of(MemberSuccess.MEMBER_EMAIL_UPDATE_SUCCESS);
	}

	@PutMapping("/api/v1/members/phone-number")
	public SuccessResponse<?> updatePhoneNumber(@RequestBody @Valid UpdatePhoneNumberRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		memberUpdateService.updatePhoneNumber(request, memberNo);

		return SuccessResponse.of(MemberSuccess.MEMBER_PHONENUMBER_UPDATE_SUCCESS);
	}

	@PutMapping("/api/v1/members/password")
	public SuccessResponse<?> updatePassword(@RequestBody @Valid UpdatePasswordRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		memberUpdateService.updatePassword(request, memberNo);

		return SuccessResponse.of(MemberSuccess.MEMBER_PASSWORD_UPDATE_SUCCESS);
	}

}
